"""カテゴリ別分析履歴（レポート）ウィンドウ"""
import customtkinter as ctk
from datetime import date, datetime
from matplotlib.figure import Figure
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
import matplotlib

class CategoryReportWindow(ctk.CTkToplevel):
    def __init__(self, parent, app, category):
        super().__init__(parent)
        self.app = app
        self.category = category
        
        self.title(f"{category.name} のレポート")
        self.geometry("550x700")
        self.minsize(450, 600)
        
        # モーダル
        self.grab_set()

        self._build_ui()
        self._load_data()

    def _build_ui(self):
        # === バーチャート領域 ===
        chart_frame = ctk.CTkFrame(self)
        chart_frame.pack(fill="x", padx=10, pady=(10, 5))

        self.fig = Figure(figsize=(5, 3), dpi=100, facecolor='#2b2b2b')
        self.ax = self.fig.add_subplot(111)
        self.canvas = FigureCanvasTkAgg(self.fig, master=chart_frame)
        self.canvas.get_tk_widget().pack(fill="both", expand=True, padx=5, pady=5)

        # === 履歴リスト領域 ===
        list_container = ctk.CTkFrame(self)
        list_container.pack(fill="both", expand=True, padx=10, pady=(5, 10))
        
        ctk.CTkLabel(list_container, text="全期間の履歴", font=("", 14, "bold")).pack(anchor="w", padx=10, pady=(10, 0))

        self.list_frame = ctk.CTkScrollableFrame(list_container)
        self.list_frame.pack(fill="both", expand=True, padx=5, pady=5)

    def _load_data(self):
        daily_data = self.app.get_active_daily_data()
        cat_data = [d for d in daily_data if d.category_sync_id == self.category.sync_id]
        
        if not cat_data:
            self.ax.text(0.5, 0.5, "データなし", ha='center', va='center',
                         fontsize=14, color='gray', transform=self.ax.transAxes)
            self.ax.set_facecolor('#2b2b2b')
            self.canvas.draw()
            ctk.CTkLabel(self.list_frame, text="履歴がありません").pack(pady=20)
            return

        # 日付で降順ソート
        cat_data.sort(key=lambda d: d.date, reverse=True)

        # === バーチャートの描画 (月毎の集計) ===
        monthly_sums = {}
        for d in cat_data:
            try:
                parts = d.date.split("-")
                ym = f"{parts[0]}/{int(parts[1])}" # "YYYY/M"
            except:
                ym = "Unknown"
            
            monthly_sums[ym] = monthly_sums.get(ym, 0) + d.amount

        # 古い順にソートしてX軸とする
        sorted_ym = sorted(monthly_sums.keys())
        amounts = [monthly_sums[ym] for ym in sorted_ym]

        self.ax.clear()
        
        try:
            bar_color = f"#{self.category.color_code}"
            int(self.category.color_code, 16)
        except ValueError:
            bar_color = "#2196F3"

        bars = self.ax.bar(sorted_ym, amounts, color=bar_color)
        
        # 見栄えの調整
        self.ax.set_facecolor('#2b2b2b')
        self.ax.tick_params(colors='white', axis='both', labelsize=10)
        for spine in self.ax.spines.values():
            spine.set_color('#555555')
        
        # 上部の枠線を消す
        self.ax.spines['top'].set_visible(False)
        self.ax.spines['right'].set_visible(False)

        # バーの上に値を表示
        for bar in bars:
            height = bar.get_height()
            self.ax.text(bar.get_x() + bar.get_width()/2., height,
                         f'¥{int(height):,}',
                         ha='center', va='bottom', color='white', fontsize=9)

        # X軸ラベルが傾かないように、表示数が多い場合はスキップ等するが簡易的にすべて表示
        self.fig.autofmt_xdate(rotation=45)
        self.fig.tight_layout()
        self.canvas.draw()

        # === 履歴リストの描画 ===
        current_date_str = None

        for d in cat_data:
            if d.date != current_date_str:
                current_date_str = d.date
                try:
                    parts = d.date.split("-")
                    dt = date(int(parts[0]), int(parts[1]), int(parts[2]))
                    weekdays = ["月", "火", "水", "木", "金", "土", "日"]
                    header_text = f"{dt.year}年{dt.month}月{dt.day}日 ({weekdays[dt.weekday()]})"
                except (ValueError, IndexError):
                    header_text = d.date

                # その日のカテゴリ収支合計 (このカテゴリのみ)
                day_entries = [x for x in cat_data if x.date == d.date]
                day_total = sum(x.amount for x in day_entries)
                
                header_frame = ctk.CTkFrame(self.list_frame, fg_color="#333333")
                header_frame.pack(fill="x", pady=(8, 2))
                ctk.CTkLabel(header_frame, text=header_text, font=("", 13, "bold"),
                              anchor="w").pack(side="left", padx=10, pady=4)
                
                color = "#4FC3F7" if d.type == "INCOME" else "#EF5350"
                sign = "+" if d.type == "INCOME" else "-"
                ctk.CTkLabel(header_frame, text=f"{sign}¥{day_total:,}",
                              font=("", 13), text_color=color,
                              anchor="e").pack(side="right", padx=10, pady=4)

            # データ行
            row_frame = ctk.CTkFrame(self.list_frame, fg_color="transparent")
            row_frame.pack(fill="x", pady=1)

            # メモ
            ctk.CTkLabel(row_frame, text=d.memo if d.memo else "(メモなし)", anchor="w",
                          font=("", 12)).pack(side="left", fill="x", expand=True, padx=15, pady=2)

            amount_color = "#4FC3F7" if d.type == "INCOME" else "#EF5350"
            sign = "+" if d.type == "INCOME" else "-"
            ctk.CTkLabel(row_frame, text=f"{sign}¥{d.amount:,}", anchor="e",
                          font=("", 12), text_color=amount_color).pack(side="right", padx=10, pady=2)
