"""カレンダータブ - 月間カレンダーと収支リスト"""
import customtkinter as ctk
import tkinter as tk
from datetime import date, timedelta
from data.models import _now_millis
from ui.dialog_utils import (parse_date, parse_amount, make_error_label, setup_modal,
                             theme_row_colors)
import calendar


class CalendarTab:
    # 日別リストの初回描画件数。1か月分を一度に描画すると800個以上のウィジェットに
    # なり1秒前後かかるため、まず直近分だけ描画して残りはボタンで追加する。
    INITIAL_ROWS = 40
    MORE_ROWS = 60

    def __init__(self, parent, app):
        self.parent = parent
        self.app = app
        self.current_year = date.today().year
        self.current_month = date.today().month
        self._visible_rows = self.INITIAL_ROWS

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
        self.cal_frame = ctk.CTkFrame(self.parent, height=310)
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

    def _show_more(self):
        self._visible_rows += self.MORE_ROWS
        self._update_daily_list()

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
        self._visible_rows = self.INITIAL_ROWS
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
                ctk.CTkLabel(cell, text=str(day), font=("", 12, "bold"), height=16,
                              text_color=day_color).pack(anchor="nw", padx=3, pady=(2, 0))

                inc, exp = day_totals.get(day, (0, 0))
                if inc > 0:
                    ctk.CTkLabel(cell, text=f"+{inc:,}", font=("", 10), height=14,
                                  text_color="#4FC3F7", anchor="e").pack(fill="x", padx=3, pady=0)
                if exp > 0:
                    ctk.CTkLabel(cell, text=f"-{exp:,}", font=("", 10), height=14,
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

        # 日ごとの合計を先に1回だけ求める（ヘッダー描画のたびに月全体を
        # 走査すると件数の2乗のコストになるため）
        day_sums = {}
        for x in month_data:
            inc, exp = day_sums.get(x.date, (0, 0))
            if x.type == "INCOME":
                inc += x.amount
            else:
                exp += x.amount
            day_sums[x.date] = (inc, exp)

        total_rows = len(month_data)
        visible = month_data[:self._visible_rows]
        current_date_str = None
        row_bg, row_fg = theme_row_colors(self.list_frame)

        for d in visible:
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
                day_income, day_expense = day_sums.get(d.date, (0, 0))
                day_total = day_income - day_expense
                sign = "+" if day_total >= 0 else ""

                header_frame = tk.Frame(self.list_frame, bg="#333333")
                header_frame.pack(fill="x", pady=(8, 2))
                tk.Label(header_frame, text=header_text, font=("", 13, "bold"),
                          bg="#333333", fg="#FFFFFF", anchor="w").pack(
                    side="left", padx=10, pady=4)
                color = "#4FC3F7" if day_total >= 0 else "#EF5350"
                tk.Label(header_frame, text=f"{sign}¥{day_total:,}", font=("", 13),
                          bg="#333333", fg=color, anchor="e").pack(
                    side="right", padx=10, pady=4)

            # データ行
            cat = self.app.get_category_by_sync_id(d.category_sync_id)
            cat_name = cat.name if cat else "?"
            try:
                cat_color = f"#{cat.color_code}" if cat else "#808080"
                int(cat.color_code, 16)
            except (ValueError, AttributeError):
                cat_color = "#808080"

            row_frame = tk.Frame(self.list_frame, bg=row_bg)
            row_frame.pack(fill="x", pady=1)

            # カテゴリ色インジケーター
            tk.Frame(row_frame, width=6, height=16, bg=cat_color).pack(
                side="left", padx=(10, 5))

            tk.Label(row_frame, text=cat_name, width=10, anchor="w", font=("", 12),
                      bg=row_bg, fg=row_fg).pack(side="left", padx=2, pady=2)
            tk.Label(row_frame, text=d.memo, anchor="w", font=("", 12),
                      bg=row_bg, fg=row_fg).pack(
                side="left", fill="x", expand=True, padx=5, pady=2)

            # 右端から [金額][支払方法][編集][削除] の順になるよう、逆順でpackする
            # （side="right" は先にpackしたものが右端に来る）
            tk.Button(row_frame, text="削除", font=("", 10), width=4,
                       bg="#555555", fg="#FFFFFF", activebackground="#EF5350",
                       activeforeground="#FFFFFF", relief="flat", bd=0, cursor="hand2",
                       command=lambda entry=d: self._delete_entry(entry)).pack(
                side="right", padx=(2, 10), pady=2)

            tk.Button(row_frame, text="編集", font=("", 10), width=4,
                       bg="#555555", fg="#FFFFFF", activebackground="#4FC3F7",
                       activeforeground="#FFFFFF", relief="flat", bd=0, cursor="hand2",
                       command=lambda entry=d: self._edit_entry(entry)).pack(
                side="right", padx=2, pady=2)

            # 支払方法ラベル（未設定でも幅を確保して金額列の位置を揃える）
            if d.payment_method:
                pm_text = "💴" if d.payment_method == "CASH" else "💳"
            else:
                pm_text = ""
            tk.Label(row_frame, text=pm_text, width=3, anchor="center", font=("", 12),
                      bg=row_bg, fg=row_fg).pack(side="right", padx=(0, 2), pady=2)

            # 金額（幅を固定して行間で右揃えを維持）
            amount_color = "#4FC3F7" if d.type == "INCOME" else "#EF5350"
            sign = "+" if d.type == "INCOME" else "-"
            tk.Label(row_frame, text=f"{sign}¥{d.amount:,}", width=12, anchor="e",
                      font=("", 12), bg=row_bg, fg=amount_color).pack(
                side="right", padx=(10, 5), pady=2)

        # 残りがある場合だけ追加読み込みのボタンを出す
        remaining = total_rows - len(visible)
        if remaining > 0:
            ctk.CTkButton(
                self.list_frame,
                text=f"残り {remaining} 件を表示",
                height=28, fg_color="#555555", hover_color="#4FC3F7",
                command=self._show_more,
            ).pack(fill="x", padx=40, pady=(10, 4))

    def _delete_entry(self, entry):
        from tkinter import messagebox
        sync_id = entry.sync_id
        if messagebox.askyesno("確認", "このデータを削除しますか？"):
            # 確認ダイアログ表示中に再読み込みが走っている可能性があるため引き直す
            target = self._find_daily_data(sync_id)
            if target is None:
                self.refresh()
                return
            target.is_deleted = True
            target.updated_at = _now_millis()
            self.app.save_data()
            self.refresh()

    def _find_daily_data(self, sync_id):
        """syncIdで収支データを引き直す（バックグラウンド再読み込みで
        オブジェクトが差し替わっている場合に備える）"""
        for d in self.app.data.daily_data:
            if d.sync_id == sync_id:
                return d
        return None

    def _edit_entry(self, entry):
        sync_id = entry.sync_id
        dialog = ctk.CTkToplevel(self.parent)
        setup_modal(dialog, self.parent, "収支データの編集", 420, 500)

        body = ctk.CTkFrame(dialog, fg_color="transparent")
        body.pack(fill="both", expand=True)

        ctk.CTkLabel(body, text="日付 (YYYY-MM-DD):", anchor="w").pack(fill="x", padx=20, pady=(10, 0))
        date_var = ctk.StringVar(value=entry.date)
        date_entry = ctk.CTkEntry(body, textvariable=date_var, placeholder_text="2026-01-01")
        date_entry.pack(pady=5, padx=20, fill="x")

        ctk.CTkLabel(body, text="カテゴリ:", anchor="w").pack(fill="x", padx=20, pady=(10, 0))
        cats = self.app.get_active_categories(entry.type)
        cat_names = [c.name for c in cats]
        current_cat = self.app.get_category_by_sync_id(entry.category_sync_id)
        current_cat_name = current_cat.name if current_cat else (cat_names[0] if cat_names else "")
        cat_var = ctk.StringVar(value=current_cat_name)
        if cat_names:
            ctk.CTkOptionMenu(body, values=cat_names, variable=cat_var).pack(pady=5, padx=20, fill="x")
        else:
            ctk.CTkLabel(body, text="※ このタイプのカテゴリが未登録です",
                          text_color="#888888", anchor="w").pack(fill="x", padx=20, pady=5)

        ctk.CTkLabel(body, text="メモ:", anchor="w").pack(fill="x", padx=20, pady=(10, 0))
        memo_var = ctk.StringVar(value=entry.memo)
        ctk.CTkEntry(body, textvariable=memo_var).pack(pady=5, padx=20, fill="x")

        ctk.CTkLabel(body, text="金額:", anchor="w").pack(fill="x", padx=20, pady=(10, 0))
        amount_var = ctk.StringVar(value=str(entry.amount))
        ctk.CTkEntry(body, textvariable=amount_var).pack(pady=5, padx=20, fill="x")

        pm_var = ctk.StringVar(value=entry.payment_method if entry.payment_method else "CASH")
        if entry.type == "EXPENSE":
            ctk.CTkLabel(body, text="支払方法:", anchor="w").pack(fill="x", padx=20, pady=(10, 0))
            pm_frame = ctk.CTkFrame(body, fg_color="transparent")
            pm_frame.pack(fill="x", padx=20, pady=5)
            ctk.CTkRadioButton(pm_frame, text="現金", variable=pm_var, value="CASH").pack(side="left", padx=(0, 20))
            ctk.CTkRadioButton(pm_frame, text="カード", variable=pm_var, value="CARD").pack(side="left")

        show_error, clear_error = make_error_label(body)

        def on_save():
            clear_error()

            date_val, err = parse_date(date_var.get(), "日付")
            if err:
                show_error(err)
                date_entry.focus_set()
                return

            amount_val, err = parse_amount(amount_var.get(), "金額")
            if err:
                show_error(err)
                return

            target = self._find_daily_data(sync_id)
            if target is None:
                show_error("このデータは他の端末で変更または削除されました。画面を更新します。")
                self.refresh()
                dialog.after(1200, dialog.destroy)
                return

            target.date = date_val
            target.memo = memo_var.get().strip()
            target.amount = amount_val

            sel_cat_name = cat_var.get()
            for c in cats:
                if c.name == sel_cat_name:
                    target.category_sync_id = c.sync_id
                    break

            if target.type == "EXPENSE":
                target.payment_method = pm_var.get()
            else:
                target.payment_method = None

            target.updated_at = _now_millis()

            self.app.save_data()
            self.refresh()
            dialog.destroy()

        btn_frame = ctk.CTkFrame(dialog, fg_color="transparent")
        btn_frame.pack(pady=(5, 15))
        ctk.CTkButton(btn_frame, text="保存", width=100, command=on_save).pack(side="left", padx=10)
        ctk.CTkButton(btn_frame, text="キャンセル", width=100, fg_color="#555555",
                       hover_color="#333333", command=dialog.destroy).pack(side="left", padx=10)

        dialog.bind("<Return>", lambda _e: on_save())
        date_entry.focus_set()
