"""レポートタブ - 円グラフとカテゴリ別集計"""
import customtkinter as ctk
from datetime import date
from matplotlib.figure import Figure
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
import matplotlib
matplotlib.use("TkAgg")
# 日本語フォント設定
matplotlib.rcParams['font.family'] = 'Yu Gothic'
matplotlib.rcParams['axes.unicode_minus'] = False

from ui.category_report_window import CategoryReportWindow



class ReportTab:
    def __init__(self, parent, app):
        self.parent = parent
        self.app = app
        self.current_year = date.today().year
        self.current_month = date.today().month
        self.current_type = "EXPENSE"

        self._build_ui()

    def _build_ui(self):
        # === 年月ナビゲーション ===
        nav_frame = ctk.CTkFrame(self.parent)
        nav_frame.pack(fill="x", padx=10, pady=(10, 5))

        ctk.CTkButton(nav_frame, text="◀", width=40,
                       command=self._prev_month).pack(side="left", padx=5, pady=5)

        self.month_label = ctk.CTkLabel(nav_frame, text="", font=("", 18, "bold"))
        self.month_label.pack(side="left", expand=True, padx=5, pady=5)

        ctk.CTkButton(nav_frame, text="▶", width=40,
                       command=self._next_month).pack(side="left", padx=5, pady=5)

        # === サマリー ===
        summary_frame = ctk.CTkFrame(self.parent)
        summary_frame.pack(fill="x", padx=10, pady=5)

        self.income_label = ctk.CTkLabel(summary_frame, text="収入\n¥0",
                                          font=("", 14), text_color="#4FC3F7")
        self.income_label.pack(side="left", expand=True, padx=5, pady=8)

        self.expense_label = ctk.CTkLabel(summary_frame, text="支出\n-¥0",
                                           font=("", 14), text_color="#EF5350")
        self.expense_label.pack(side="left", expand=True, padx=5, pady=8)

        self.total_label = ctk.CTkLabel(summary_frame, text="収支\n¥0",
                                         font=("", 14, "bold"))
        self.total_label.pack(side="left", expand=True, padx=5, pady=8)

        # === 支出/収入 切り替え ===
        type_frame = ctk.CTkFrame(self.parent)
        type_frame.pack(fill="x", padx=10, pady=5)

        self.btn_expense = ctk.CTkButton(
            type_frame, text="支出", width=120,
            command=lambda: self._set_type("EXPENSE"),
            fg_color="#E53935", hover_color="#C62828"
        )
        self.btn_expense.pack(side="left", padx=5, pady=5)

        self.btn_income = ctk.CTkButton(
            type_frame, text="収入", width=120,
            command=lambda: self._set_type("INCOME"),
            fg_color="#555555", hover_color="#333333"
        )
        self.btn_income.pack(side="left", padx=5, pady=5)

        # === メインコンテンツ (上: グラフ、下: リスト) ===
        content_frame = ctk.CTkFrame(self.parent, fg_color="transparent")
        content_frame.pack(fill="both", expand=True, padx=10, pady=5)

        # 円グラフ
        chart_frame = ctk.CTkFrame(content_frame)
        chart_frame.pack(side="top", fill="both", expand=True, padx=5, pady=(0, 5))

        self.fig = Figure(figsize=(4, 3.5), dpi=100, facecolor='#2b2b2b')
        self.ax = self.fig.add_subplot(111)
        self.canvas = FigureCanvasTkAgg(self.fig, master=chart_frame)
        self.canvas.get_tk_widget().pack(fill="both", expand=True, padx=5, pady=5)

        # カテゴリリスト
        self.list_frame = ctk.CTkScrollableFrame(content_frame)
        self.list_frame.pack(side="top", fill="both", expand=True, padx=0, pady=(5, 0))

    def _set_type(self, t: str):
        self.current_type = t
        if t == "EXPENSE":
            self.btn_expense.configure(fg_color="#E53935")
            self.btn_income.configure(fg_color="#555555")
        else:
            self.btn_expense.configure(fg_color="#555555")
            self.btn_income.configure(fg_color="#1976D2")
        self.refresh()

    def _prev_month(self):
        if self.current_month == 1:
            self.current_month = 12
            self.current_year -= 1
        else:
            self.current_month -= 1
        self.refresh()

    def _next_month(self):
        if self.current_month == 12:
            self.current_month = 1
            self.current_year += 1
        else:
            self.current_month += 1
        self.refresh()

    def refresh(self):
        self.month_label.configure(text=f"{self.current_year}/{self.current_month:02d}")
        self._update_report()

    def _update_report(self):
        daily_data = self.app.get_active_daily_data()

        # 今月のデータをフィルタ
        month_data = []
        for d in daily_data:
            try:
                parts = d.date.split("-")
                y, m = int(parts[0]), int(parts[1])
            except (ValueError, IndexError):
                continue
            if y == self.current_year and m == self.current_month:
                month_data.append(d)

        total_income = sum(d.amount for d in month_data if d.type == "INCOME")
        total_expense = sum(d.amount for d in month_data if d.type == "EXPENSE")
        total = total_income - total_expense

        # サマリー更新
        self.income_label.configure(text=f"収入\n+¥{total_income:,}")
        self.expense_label.configure(text=f"支出\n-¥{total_expense:,}")
        sign = "+" if total >= 0 else ""
        self.total_label.configure(
            text=f"収支\n{sign}¥{total:,}",
            text_color="#4FC3F7" if total >= 0 else "#EF5350"
        )

        # カテゴリ別集計
        target_data = [d for d in month_data if d.type == self.current_type]
        cat_totals = {}  # sync_id -> amount
        for d in target_data:
            cat_totals[d.category_sync_id] = cat_totals.get(d.category_sync_id, 0) + d.amount

        total_amount = sum(cat_totals.values())

        # カテゴリ情報を付加してソート
        report_items = []
        for sync_id, amount in cat_totals.items():
            cat = self.app.get_category_by_sync_id(sync_id)
            if cat:
                pct = amount / total_amount if total_amount > 0 else 0
                report_items.append((cat, amount, pct))

        report_items.sort(key=lambda x: x[1], reverse=True)

        # 円グラフ更新
        self._update_chart(report_items, total_amount)

        # リスト更新
        self._update_list(report_items, total_amount)

    def _update_chart(self, items, total):
        self.ax.clear()

        if not items:
            self.ax.text(0.5, 0.5, "データなし", ha='center', va='center',
                         fontsize=14, color='gray', transform=self.ax.transAxes)
            self.canvas.draw()
            return

        labels = [item[0].name for item in items]
        sizes = [item[1] for item in items]
        colors = []
        for item in items:
            try:
                colors.append(f"#{item[0].color_code}")
            except Exception:
                colors.append("#808080")

        wedges, texts, autotexts = self.ax.pie(
            sizes, labels=labels, colors=colors,
            autopct='%1.1f%%', pctdistance=0.85,
            textprops={'color': 'white', 'fontsize': 10}
        )

        # 中心に合計を表示
        centre_circle = matplotlib.patches.Circle((0, 0), 0.55, fc='#2b2b2b')
        self.ax.add_artist(centre_circle)
        self.ax.text(0, 0, f"¥{total:,}", ha='center', va='center',
                      fontsize=14, color='white', fontweight='bold')

        self.ax.set_facecolor('#2b2b2b')
        self.fig.set_facecolor('#2b2b2b')
        self.canvas.draw()

    def _update_list(self, items, total_amount):
        # クリア
        for widget in self.list_frame.winfo_children():
            widget.destroy()

        # 予算設定の取得
        quotas = {q.category_sync_id: q for q in self.app.get_active_quota_settings()}

        for cat, amount, pct in items:
            row_frame = ctk.CTkFrame(self.list_frame, fg_color="#333333", corner_radius=8)
            row_frame.pack(fill="x", pady=2, padx=2)

            try:
                cat_color = f"#{cat.color_code}"
                int(cat.color_code, 16)
            except (ValueError, AttributeError):
                cat_color = "#808080"

            # === カラーインジケーター ===
            indicator = ctk.CTkFrame(row_frame, width=12, height=40, fg_color=cat_color, corner_radius=6)
            indicator.pack(side="left", padx=5, pady=5)

            # === カテゴリ名と詳細（予算情報など） ===
            info_frame = ctk.CTkFrame(row_frame, fg_color="transparent")
            info_frame.pack(side="left", fill="both", expand=True, padx=5, pady=5)

            ctk.CTkLabel(info_frame, text=cat.name, font=("", 14, "bold"), anchor="w").pack(fill="x")
            
            quota = quotas.get(cat.sync_id)
            if quota and quota.amount > 0:
                remaining = quota.amount - amount
                if remaining >= 0:
                    q_text = f"予算: ¥{quota.amount:,} | 残額: ¥{remaining:,}"
                    q_color = "#AAAAAA"
                else:
                    q_text = f"予算: ¥{quota.amount:,} | 超過: ¥{abs(remaining):,}"
                    q_color = "#EF5350"
                ctk.CTkLabel(info_frame, text=q_text, font=("", 11), text_color=q_color, anchor="w").pack(fill="x")

            # === 金額とパーセンテージ ===
            right_frame = ctk.CTkFrame(row_frame, fg_color="transparent")
            right_frame.pack(side="right", fill="y", padx=5, pady=5)
            
            ctk.CTkLabel(right_frame, text=f"¥ {amount:,}", font=("", 14, "bold")).pack(anchor="e")
            ctk.CTkLabel(right_frame, text=f"{pct*100:.1f}%", font=("", 12), text_color="#AAAAAA").pack(anchor="e")

            # クリックイベントの登録処理
            def bind_click(widget, cat_obj=cat):
                widget.bind("<Button-1>", lambda e, c=cat_obj: self._open_category_report(c))
                for child in widget.winfo_children():
                    bind_click(child, cat_obj)
            
            bind_click(row_frame)
            
            # ホバー時のカーソル変更 (Windows等でクリック可能であることを示す)
            row_frame.configure(cursor="hand2")

    def _open_category_report(self, category):
        # 既に開いている場合は閉じて新しく開く（再描画のため）
        if hasattr(self, "report_window") and self.report_window.winfo_exists():
            self.report_window.destroy()
        self.report_window = CategoryReportWindow(self.parent.winfo_toplevel(), self.app, category)



# matplotlib patches import
import matplotlib.patches
