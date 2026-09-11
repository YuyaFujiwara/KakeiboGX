"""メインウィンドウ - タブ切り替え"""
# pyrefly: ignore [missing-import]
import customtkinter as ctk
from data.models import SyncData
from data.sync_file import load_sync_file, save_sync_file
from ui.input_tab import InputTab
from ui.calendar_tab import CalendarTab
from ui.report_tab import ReportTab
from ui.settings_tab import SettingsTab
import config


class App(ctk.CTk):
    TAB_NAMES = ("入力", "カレンダー", "レポート", "設定")

    def __init__(self):
        super().__init__()
        # データ更新後、まだ再描画していないタブの名前。
        # 表示中のタブ以外は切り替え時にまとめて更新する。
        self._dirty_tabs = set()
        self.title(config.APP_NAME)
        self.geometry(f"{config.WINDOW_WIDTH}x{config.WINDOW_HEIGHT}")
        self.minsize(900, 600)

        # データ読み込み
        self.sync_file_path = config.SYNC_FILE_PATH
        
        # Googleドライブがマウントされているかチェック（パスのドライブレターが存在するか）
        import os
        from tkinter import messagebox
        drive_letter = os.path.splitdrive(self.sync_file_path)[0]
        if drive_letter and not os.path.exists(drive_letter + "\\"):
            messagebox.showwarning(
                "Googleドライブ未接続", 
                f"同期先のドライブ ({drive_letter}) が見つかりません。\nGoogle Drive for Desktopが起動しているか確認してください。\n\n※このまま起動するとデータは空になります。"
            )

        loaded_data = load_sync_file(self.sync_file_path)
        self.data: SyncData = loaded_data if loaded_data is not None else SyncData()

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

        # 入力タブ以外は中身を空のまま構築され、初回表示時の refresh() で
        # 描画される。起動直後に描画されるよう dirty として登録しておく。
        self._dirty_tabs.update(self.TAB_NAMES)

        # バックグラウンドタスク開始
        self.last_sync_mtime = 0
        self._start_background_tasks()

    def _start_background_tasks(self):
        self._check_file_update()
        self._check_date_change()

    def _check_file_update(self):
        import os
        if os.path.exists(self.sync_file_path):
            current_mtime = os.path.getmtime(self.sync_file_path)
            if self.last_sync_mtime != 0 and self.last_sync_mtime < current_mtime:
                # 自分が保存した直後か外部の変更かに関わらずリロード（自分が保存した場合は中身は同じなので安全）
                # ユーザーが編集中の場合は競合する可能性があるが、簡易的な同期としてリロードする
                self.reload_data()
            self.last_sync_mtime = current_mtime
        self.after(5000, self._check_file_update)

    def _check_date_change(self):
        from datetime import date
        today = date.today()
        # 日を跨いだ場合にInputTabの日付を更新し、固定費を再チェック
        if hasattr(self, 'input_tab') and self.input_tab.current_date < today:
            self.input_tab.current_date = today
            self.input_tab.refresh()
            self._check_and_apply_fixed_costs()
        self.after(60000, self._check_date_change)

    def _on_tab_changed(self):
        """タブが切り替わった時にデータを再描画"""
        self._refresh_tab(self.tabview.get())

    def _tab_widgets(self):
        return dict(zip(self.TAB_NAMES, (self.input_tab, self.calendar_tab,
                                          self.report_tab, self.settings_tab)))

    def _refresh_tab(self, name, force=False):
        """指定タブを再描画する。force=False なら未更新(dirty)のときだけ行う。"""
        tab = self._tab_widgets().get(name)
        if tab is None:
            return
        if force or name in self._dirty_tabs:
            tab.refresh()
            self._dirty_tabs.discard(name)

    def save_data(self):
        """データをJSONファイルに保存"""
        save_sync_file(self.sync_file_path, self.data)
        import os
        if os.path.exists(self.sync_file_path):
            self.last_sync_mtime = os.path.getmtime(self.sync_file_path)

        # 変更したタブは自分で再描画するが、他のタブは表示が古くなるため
        # dirty にしておき、切り替え時に更新する。
        self._dirty_tabs.update(self.TAB_NAMES)

    def reload_data(self):
        """JSONファイルからデータを再読み込み"""
        import os
        new_data = load_sync_file(self.sync_file_path)
        if new_data is None:
            return  # 読み込みエラー時は現在のデータを保持する
        
        self.data = new_data
        if os.path.exists(self.sync_file_path):
            self.last_sync_mtime = os.path.getmtime(self.sync_file_path)
        self._refresh_all()

    def _refresh_all(self):
        """全タブのデータを更新する。

        全タブを即座に作り直すと実データ量で2秒近くかかるため、表示中のタブだけ
        その場で更新し、残りは dirty として記録してタブ切り替え時に更新する。
        """
        self._dirty_tabs.update(self.TAB_NAMES)
        self._refresh_tab(self.tabview.get())

    def _check_and_apply_fixed_costs(self):
        from datetime import date, timedelta
        import calendar
        from data.models import DailyData, _now_millis
        
        today = date.today()
        _, last_day = calendar.monthrange(today.year, today.month)
        month_end = date(today.year, today.month, last_day)
        
        changed = False

        for setting in self.data.fixed_cost_settings:
            if setting.is_deleted:
                continue
            
            try:
                start_date = date.fromisoformat(setting.start_date)
            except ValueError:
                continue  # 無効な開始日
                
            end_limit = month_end
            if setting.end_date:
                try:
                    ed = date.fromisoformat(setting.end_date)
                    if ed < month_end:
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
                        category_sync_id=setting.category_sync_id,
                        payment_method=setting.payment_method
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

    def get_quota_for_category(self, category_sync_id: str):
        """カテゴリの予算設定を1件だけ返す。

        同じカテゴリに複数の予算設定が存在しうる（アプリ再インストール等で
        syncId違いの行が同期経由で増えることがある）ため、updatedAt が最新の
        ものを採用する。同値の場合は syncId 順で決めることで、PC と Android で
        必ず同じ行が選ばれるようにする。
        """
        candidates = [q for q in self.data.quota_settings
                      if not q.is_deleted and q.category_sync_id == category_sync_id]
        if not candidates:
            return None
        return max(candidates, key=lambda q: (q.updated_at, q.sync_id))

    def get_category_by_sync_id(self, sync_id: str):
        """syncIdからカテゴリを取得"""
        for c in self.data.categories:
            if c.sync_id == sync_id:
                return c
        return None
