"""カレンダータブ - 月間カレンダーと収支リスト"""
import customtkinter as ctk
from datetime import date, timedelta
from data.models import _now_millis
import calendar


class CalendarTab:
    def __init__(self, parent, app):
        self.parent = parent
        self.app = app
        self.current_year = date.today().year
        self.current_month = date.today().month

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

        # === カレンダーグリッド ===
        self.cal_frame = ctk.CTkFrame(self.parent, height=270)
        self.cal_frame.grid_propagate(False)
        self.cal_frame.pack(fill="x", padx=10, pady=5)

        # 曜日ヘッダー
        weekdays = ["月", "火", "水", "木", "金", "土", "日"]
        for i, wd in enumerate(weekdays):
            color = "#FF6B6B" if i == 6 else ("#6BA3FF" if i == 5 else "#CCCCCC")
            lbl = ctk.CTkLabel(self.cal_frame, text=wd, font=("", 12, "bold"),
                                text_color=color)
            lbl.grid(row=0, column=i, padx=2, pady=2, sticky="nsew")

        for i in range(7):
            self.cal_frame.columnconfigure(i, weight=1, uniform="cal_col", minsize=50)

        # 6行分の高さを均等に分割（全体の高さはcal_frameのheightで固定）
        for i in range(1, 7):
            self.cal_frame.rowconfigure(i, weight=1, uniform="cal_row")

        # === 月間サマリー ===
        summary_frame = ctk.CTkFrame(self.parent)
        summary_frame.pack(fill="x", padx=10, pady=5)

        self.income_label = ctk.CTkLabel(summary_frame, text="収入: ¥0",
                                          font=("", 14), text_color="#4FC3F7")
        self.income_label.pack(side="left", expand=True, padx=5, pady=5)

        self.expense_label = ctk.CTkLabel(summary_frame, text="支出: -¥0",
                                           font=("", 14), text_color="#EF5350")
        self.expense_label.pack(side="left", expand=True, padx=5, pady=5)

        self.total_label = ctk.CTkLabel(summary_frame, text="収支: ¥0",
                                         font=("", 14, "bold"))
        self.total_label.pack(side="left", expand=True, padx=5, pady=5)

        # === 日別収支リスト ===
        self.list_frame = ctk.CTkScrollableFrame(self.parent)
        self.list_frame.pack(fill="both", expand=True, padx=10, pady=(5, 10))

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
        self._update_month_label()
        self._update_calendar()
        self._update_daily_list()

    def _update_month_label(self):
        self.month_label.configure(text=f"{self.current_year}/{self.current_month:02d}")

    def _update_calendar(self):
        # 既存のセルをクリア（曜日ヘッダー以外）
        for widget in self.cal_frame.winfo_children():
            info = widget.grid_info()
            if info.get("row", 0) > 0:
                widget.destroy()

        # 月のデータを集計
        daily_data = self.app.get_active_daily_data()
        day_totals = {}  # day -> (income, expense)
        total_income = 0
        total_expense = 0

        for d in daily_data:
            try:
                parts = d.date.split("-")
                y, m, day = int(parts[0]), int(parts[1]), int(parts[2])
            except (ValueError, IndexError):
                continue
            if y == self.current_year and m == self.current_month:
                inc, exp = day_totals.get(day, (0, 0))
                if d.type == "INCOME":
                    inc += d.amount
                    total_income += d.amount
                else:
                    exp += d.amount
                    total_expense += d.amount
                day_totals[day] = (inc, exp)

        # サマリー更新
        self.income_label.configure(text=f"収入: ¥{total_income:,}")
        self.expense_label.configure(text=f"支出: -¥{total_expense:,}")
        total = total_income - total_expense
        sign = "+" if total >= 0 else ""
        self.total_label.configure(
            text=f"収支: {sign}¥{total:,}",
            text_color="#4FC3F7" if total >= 0 else "#EF5350"
        )

        # カレンダーセルを作成
        cal = calendar.Calendar(firstweekday=0)  # 月曜始まり
        month_days = cal.monthdayscalendar(self.current_year, self.current_month)

        for week_idx, week in enumerate(month_days):
            for day_idx, day in enumerate(week):
                cell = ctk.CTkFrame(self.cal_frame, corner_radius=4)
                cell.grid(row=week_idx + 1, column=day_idx, padx=1, pady=1, sticky="nsew")
                cell.grid_propagate(False)
                cell.pack_propagate(False)

                if day == 0:
                    continue

                # 日付
                day_color = "#FF6B6B" if day_idx == 6 else ("#6BA3FF" if day_idx == 5 else "#CCCCCC")
                ctk.CTkLabel(cell, text=str(day), font=("", 12, "bold"),
                              text_color=day_color).pack(anchor="nw", padx=3, pady=(2, 0))

                inc, exp = day_totals.get(day, (0, 0))
                if inc > 0:
                    ctk.CTkLabel(cell, text=f"+{inc:,}", font=("", 10),
                                  text_color="#4FC3F7", anchor="e").pack(fill="x", padx=3, pady=0)
                if exp > 0:
                    ctk.CTkLabel(cell, text=f"-{exp:,}", font=("", 10),
                                  text_color="#EF5350", anchor="e").pack(fill="x", padx=3, pady=0)

    def _update_daily_list(self):
        # クリア
        for widget in self.list_frame.winfo_children():
            widget.destroy()

        daily_data = self.app.get_active_daily_data()

        # 今月のデータだけフィルタ
        month_data = []
        for d in daily_data:
            try:
                parts = d.date.split("-")
                y, m = int(parts[0]), int(parts[1])
            except (ValueError, IndexError):
                continue
            if y == self.current_year and m == self.current_month:
                month_data.append(d)

        # 日付でグループ化（降順）
        month_data.sort(key=lambda d: d.date, reverse=True)
        current_date_str = None

        for d in month_data:
            if d.date != current_date_str:
                current_date_str = d.date
                # 日付ヘッダー
                try:
                    parts = d.date.split("-")
                    dt = date(int(parts[0]), int(parts[1]), int(parts[2]))
                    weekdays = ["月", "火", "水", "木", "金", "土", "日"]
                    header_text = f"{dt.year}年{dt.month}月{dt.day}日 ({weekdays[dt.weekday()]})"
                except (ValueError, IndexError):
                    header_text = d.date

                # その日の収支合計
                day_entries = [x for x in month_data if x.date == d.date]
                day_income = sum(x.amount for x in day_entries if x.type == "INCOME")
                day_expense = sum(x.amount for x in day_entries if x.type == "EXPENSE")
                day_total = day_income - day_expense
                sign = "+" if day_total >= 0 else ""

                header_frame = ctk.CTkFrame(self.list_frame, fg_color="#333333")
                header_frame.pack(fill="x", pady=(8, 2))
                ctk.CTkLabel(header_frame, text=header_text, font=("", 13, "bold"),
                              anchor="w").pack(side="left", padx=10, pady=4)
                color = "#4FC3F7" if day_total >= 0 else "#EF5350"
                ctk.CTkLabel(header_frame, text=f"{sign}¥{day_total:,}",
                              font=("", 13), text_color=color,
                              anchor="e").pack(side="right", padx=10, pady=4)

            # データ行
            cat = self.app.get_category_by_sync_id(d.category_sync_id)
            cat_name = cat.name if cat else "?"
            try:
                cat_color = f"#{cat.color_code}" if cat else "#808080"
                int(cat.color_code, 16)
            except (ValueError, AttributeError):
                cat_color = "#808080"

            row_frame = ctk.CTkFrame(self.list_frame, fg_color="transparent")
            row_frame.pack(fill="x", pady=1)

            # カテゴリ色インジケーター
            indicator = ctk.CTkFrame(row_frame, width=6, height=16, fg_color=cat_color, corner_radius=3)
            indicator.pack(side="left", padx=(10, 5))

            ctk.CTkLabel(row_frame, text=cat_name, width=80, anchor="w",
                          font=("", 12)).pack(side="left", padx=2, pady=2)
            ctk.CTkLabel(row_frame, text=d.memo, anchor="w",
                          font=("", 12)).pack(side="left", fill="x", expand=True, padx=5, pady=2)

            amount_color = "#4FC3F7" if d.type == "INCOME" else "#EF5350"
            sign = "+" if d.type == "INCOME" else "-"
            ctk.CTkLabel(row_frame, text=f"{sign}¥{d.amount:,}", anchor="e",
                          font=("", 12), text_color=amount_color).pack(side="right", padx=10, pady=2)

            # 削除ボタン
            del_btn = ctk.CTkButton(
                row_frame, text="×", width=25, height=22,
                fg_color="#555555", hover_color="#EF5350",
                command=lambda entry=d: self._delete_entry(entry)
            )
            del_btn.pack(side="right", padx=2, pady=2)

    def _delete_entry(self, entry):
        from tkinter import messagebox
        if messagebox.askyesno("確認", "このデータを削除しますか？"):
            entry.is_deleted = True
            entry.updated_at = _now_millis()
            self.app.save_data()
            self.refresh()
