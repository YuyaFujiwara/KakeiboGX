"""カテゴリ別分析履歴（レポート）ウィンドウ"""
import customtkinter as ctk
import tkinter as tk
from datetime import date, datetime
from matplotlib.figure import Figure
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
import matplotlib
from ui.dialog_utils import theme_row_colors

class CategoryReportWindow(ctk.CTkToplevel):
    # 履歴リストの初回描画件数。カテゴリによっては全期間で1000件を超え、
    # 一度に描画すると1万個以上のウィジェットになるため分割して描画する。
    INITIAL_ROWS = 60
    MORE_ROWS = 100

    def __init__(self, parent, app, category):
        super().__init__(parent)
        self.app = app
        self.category = category
        self._cat_data = []
        self._day_sums = {}
        self._visible_rows = self.INITIAL_ROWS
        
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
        
        self.list_title = ctk.CTkLabel(list_container, text="全期間の履歴", font=("", 14, "bold"))
        self.list_title.pack(anchor="w", padx=10, pady=(10, 0))

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
        self._cat_data = cat_data
        self.list_title.configure(text=f"全期間の履歴 ({len(cat_data):,} 件)")

        # 日ごとの合計を先に1回だけ求める（ヘッダー描画のたびに全期間を
        # 走査すると件数の2乗のコストになるため）
        day_sums = {}
        for x in cat_data:
            day_sums[x.date] = day_sums.get(x.date, 0) + x.amount
        self._day_sums = day_sums

        self._render_list()

    def _show_more(self):
        self._visible_rows += self.MORE_ROWS
        self._render_list()

    def _render_list(self):
        for widget in self.list_frame.winfo_children():
            widget.destroy()

        row_bg, row_fg = theme_row_colors(self.list_frame)
        total_rows = len(self._cat_data)
        visible = self._cat_data[:self._visible_rows]
        current_date_str = None

        for d in visible:
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
                day_total = self._day_sums.get(d.date, 0)

                header_frame = tk.Frame(self.list_frame, bg="#333333")
                header_frame.pack(fill="x", pady=(8, 2))
                tk.Label(header_frame, text=header_text, font=("", 13, "bold"),
                          bg="#333333", fg="#FFFFFF", anchor="w").pack(
                    side="left", padx=10, pady=4)

                color = "#4FC3F7" if d.type == "INCOME" else "#EF5350"
                sign = "+" if d.type == "INCOME" else "-"
                tk.Label(header_frame, text=f"{sign}¥{day_total:,}", font=("", 13),
                          bg="#333333", fg=color, anchor="e").pack(
                    side="right", padx=10, pady=4)

            # データ行
            row_frame = tk.Frame(self.list_frame, bg=row_bg)
            row_frame.pack(fill="x", pady=1)

            tk.Label(row_frame, text=d.memo if d.memo else "(メモなし)", anchor="w",
                      font=("", 12), bg=row_bg, fg=row_fg).pack(
                side="left", fill="x", expand=True, padx=15, pady=2)

            amount_color = "#4FC3F7" if d.type == "INCOME" else "#EF5350"
            sign = "+" if d.type == "INCOME" else "-"
            tk.Label(row_frame, text=f"{sign}¥{d.amount:,}", anchor="e", width=12,
                      font=("", 12), bg=row_bg, fg=amount_color).pack(
                side="right", padx=10, pady=2)

        # 残りがある場合だけ追加読み込みのボタンを出す
        remaining = total_rows - len(visible)
        if remaining > 0:
            ctk.CTkButton(
                self.list_frame,
                text=f"残り {remaining} 件を表示",
                height=28, fg_color="#555555", hover_color="#4FC3F7",
                command=self._show_more,
            ).pack(fill="x", padx=40, pady=(10, 4))
