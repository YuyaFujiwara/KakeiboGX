"""メインウィンドウ - タブ切り替え"""
import customtkinter as ctk
from data.models import SyncData
from data.sync_file import load_sync_file, save_sync_file
from ui.input_tab import InputTab
from ui.calendar_tab import CalendarTab
from ui.report_tab import ReportTab
from ui.settings_tab import SettingsTab
import config


class App(ctk.CTk):
    def __init__(self):
        super().__init__()
        self.title(config.APP_NAME)
        self.geometry(f"{config.WINDOW_WIDTH}x{config.WINDOW_HEIGHT}")
        self.minsize(900, 600)

        # データ読み込み
        self.sync_file_path = config.SYNC_FILE_PATH
        self.data: SyncData = load_sync_file(self.sync_file_path)

        # 固定費の自動適用
        self._check_and_apply_fixed_costs()

        # タブビュー
        self.tabview = ctk.CTkTabview(self, anchor="nw")
        self.tabview.pack(fill="both", expand=True, padx=10, pady=10)

        self.tabview.add("入力")
        self.tabview.add("カレンダー")
        self.tabview.add("レポート")
        self.tabview.add("設定")

        # 各タブの初期化
        self.input_tab = InputTab(self.tabview.tab("入力"), self)
        self.calendar_tab = CalendarTab(self.tabview.tab("カレンダー"), self)
        self.report_tab = ReportTab(self.tabview.tab("レポート"), self)
        self.settings_tab = SettingsTab(self.tabview.tab("設定"), self)

        # タブ切り替え時にデータ更新
        self.tabview.configure(command=self._on_tab_changed)

    def _on_tab_changed(self):
        """タブが切り替わった時にデータを再描画"""
        current = self.tabview.get()
        if current == "カレンダー":
            self.calendar_tab.refresh()
        elif current == "レポート":
            self.report_tab.refresh()
        elif current == "設定":
            self.settings_tab.refresh()

    def save_data(self):
        """データをJSONファイルに保存"""
        save_sync_file(self.sync_file_path, self.data)

    def reload_data(self):
        """JSONファイルからデータを再読み込み"""
        self.data = load_sync_file(self.sync_file_path)
        self._refresh_all()

    def _refresh_all(self):
        """全タブのデータを更新"""
        self.input_tab.refresh()
        self.calendar_tab.refresh()
        self.report_tab.refresh()
        self.settings_tab.refresh()

    def _check_and_apply_fixed_costs(self):
        from datetime import date, timedelta
        import calendar
        from data.models import DailyData, _now_millis
        
        today = date.today()
        changed = False

        for setting in self.data.fixed_cost_settings:
            if setting.is_deleted:
                continue
            
            try:
                start_date = date.fromisoformat(setting.start_date)
            except ValueError:
                continue  # 無効な開始日
                
            end_limit = today
            if setting.end_date:
                try:
                    ed = date.fromisoformat(setting.end_date)
                    if ed < today:
                        end_limit = ed
                except ValueError:
                    pass
            
            check_date = start_date
            if setting.last_inserted_to_daily_data:
                try:
                    last_ins = date.fromisoformat(setting.last_inserted_to_daily_data)
                    check_date = last_ins + timedelta(days=1)
                except ValueError:
                    pass
            
            inserted = False
            new_last_inserted = setting.last_inserted_to_daily_data
            
            while check_date <= end_limit:
                _, days_in_month = calendar.monthrange(check_date.year, check_date.month)
                target_day = min(setting.day_of_month, days_in_month)
                
                if check_date.day == target_day:
                    d = DailyData(
                        date=check_date.isoformat(),
                        amount=setting.amount,
                        memo=setting.name,
                        type=setting.type,
                        category_sync_id=setting.category_sync_id
                    )
                    self.data.daily_data.append(d)
                    new_last_inserted = check_date.isoformat()
                    inserted = True
                
                check_date += timedelta(days=1)
                
            if inserted:
                setting.last_inserted_to_daily_data = new_last_inserted
                setting.updated_at = _now_millis()
                changed = True
                
        if changed:
            self.save_data()

    def get_active_categories(self, type_filter: str = None):
        """削除されていないカテゴリのリストを取得"""
        cats = [c for c in self.data.categories if not c.is_deleted]
        if type_filter:
            cats = [c for c in cats if c.type == type_filter]
        return sorted(cats, key=lambda c: c.display_order)

    def get_active_daily_data(self):
        """削除されていない収支データのリストを取得"""
        return [d for d in self.data.daily_data if not d.is_deleted]

    def get_active_presets(self, type_filter: str = None):
        """削除されていないプリセットのリストを取得"""
        presets = [p for p in self.data.presets if not p.is_deleted]
        if type_filter:
            presets = [p for p in presets if p.type == type_filter]
        return sorted(presets, key=lambda p: (-p.usage_count, p.display_order))

    def get_active_quota_settings(self):
        """削除されていない予算設定のリストを取得"""
        return [q for q in self.data.quota_settings if not q.is_deleted]

    def get_category_by_sync_id(self, sync_id: str):
        """syncIdからカテゴリを取得"""
        for c in self.data.categories:
            if c.sync_id == sync_id:
                return c
        return None
