"""入力タブ - 支出/収入の入力画面"""
import customtkinter as ctk
from datetime import date, timedelta
from data.models import DailyData, _now_millis
from tkinter import messagebox


class InputTab:
    def __init__(self, parent, app):
        self.parent = parent
        self.app = app
        self.current_date = date.today()
        self.current_type = "EXPENSE"
        self.current_amount = 0
        self.selected_category_sync_id = None

        self._build_ui()
        self.refresh()

    def _build_ui(self):
        # === 上部: 支出/収入 切り替え ===
        type_frame = ctk.CTkFrame(self.parent)
        type_frame.pack(fill="x", padx=10, pady=(10, 5))

        self.type_var = ctk.StringVar(value="EXPENSE")
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

        # === 日付 ===
        date_frame = ctk.CTkFrame(self.parent)
        date_frame.pack(fill="x", padx=10, pady=5)

        ctk.CTkButton(date_frame, text="◀", width=40,
                       command=self._prev_day).pack(side="left", padx=5, pady=5)

        self.date_label = ctk.CTkLabel(date_frame, text="", font=("", 16))
        self.date_label.pack(side="left", expand=True, padx=5, pady=5)

        ctk.CTkButton(date_frame, text="▶", width=40,
                       command=self._next_day).pack(side="left", padx=5, pady=5)

        # === メモ ===
        memo_frame = ctk.CTkFrame(self.parent)
        memo_frame.pack(fill="x", padx=10, pady=5)

        ctk.CTkLabel(memo_frame, text="メモ:").pack(side="left", padx=5, pady=5)
        self.memo_entry = ctk.CTkEntry(memo_frame, width=300, placeholder_text="何に使った？")
        self.memo_entry.pack(side="left", fill="x", expand=True, padx=5, pady=5)

        # === 金額 ===
        amount_frame = ctk.CTkFrame(self.parent)
        amount_frame.pack(fill="x", padx=10, pady=5)

        ctk.CTkLabel(amount_frame, text="金額:").pack(side="left", padx=5, pady=5)
        self.amount_entry = ctk.CTkEntry(amount_frame, width=200, placeholder_text="0")
        self.amount_entry.pack(side="left", padx=5, pady=5)
        ctk.CTkLabel(amount_frame, text="円").pack(side="left", padx=2, pady=5)

        # === プリセットボタン ===
        preset_frame = ctk.CTkFrame(self.parent)
        preset_frame.pack(fill="x", padx=10, pady=5)

        ctk.CTkLabel(preset_frame, text="プリセット:").pack(side="left", padx=5, pady=5)
        self.preset_buttons_frame = ctk.CTkScrollableFrame(preset_frame, height=40, orientation="horizontal")
        self.preset_buttons_frame.pack(side="left", fill="x", expand=True, padx=5, pady=5)

        # === カテゴリ選択 ===
        ctk.CTkLabel(self.parent, text="カテゴリ:", anchor="w").pack(fill="x", padx=15, pady=(10, 2))

        self.category_scroll = ctk.CTkScrollableFrame(self.parent, height=200)
        self.category_scroll.pack(fill="both", expand=True, padx=10, pady=5)

        self.category_buttons = []

        # === 登録ボタン ===
        self.submit_btn = ctk.CTkButton(
            self.parent, text="登録する", height=45,
            font=("", 16, "bold"),
            command=self._submit,
            fg_color="#1976D2", hover_color="#1565C0"
        )
        self.submit_btn.pack(fill="x", padx=10, pady=10)

    def _set_type(self, t: str):
        self.current_type = t
        self.selected_category_sync_id = None
        if t == "EXPENSE":
            self.btn_expense.configure(fg_color="#E53935")
            self.btn_income.configure(fg_color="#555555")
        else:
            self.btn_expense.configure(fg_color="#555555")
            self.btn_income.configure(fg_color="#1976D2")
        self._refresh_categories()
        self._refresh_presets()

    def _prev_day(self):
        self.current_date -= timedelta(days=1)
        self._update_date_label()

    def _next_day(self):
        self.current_date += timedelta(days=1)
        self._update_date_label()

    def _update_date_label(self):
        weekdays = ["月", "火", "水", "木", "金", "土", "日"]
        wd = weekdays[self.current_date.weekday()]
        self.date_label.configure(
            text=f"{self.current_date.strftime('%Y/%m/%d')} ({wd})"
        )

    def _refresh_categories(self):
        # 既存ボタンをクリア
        for widget in self.category_scroll.winfo_children():
            widget.destroy()
        self.category_buttons = []

        categories = self.app.get_active_categories(self.current_type)

        # グリッドレイアウト (4列)
        cols = 4
        for i, cat in enumerate(categories):
            row, col = divmod(i, cols)
            try:
                color = f"#{cat.color_code}"
                # 色が有効かチェック
                int(cat.color_code, 16)
            except ValueError:
                color = "#808080"

            btn = ctk.CTkButton(
                self.category_scroll,
                text=cat.name,
                width=120, height=50,
                fg_color="#444444",
                hover_color=color,
                border_width=2,
                border_color="#555555",
                command=lambda sid=cat.sync_id: self._select_category(sid),
            )
            btn.grid(row=row, column=col, padx=4, pady=4, sticky="nsew")
            self.category_buttons.append((cat.sync_id, btn, color))

    def _select_category(self, sync_id: str):
        self.selected_category_sync_id = sync_id
        for sid, btn, color in self.category_buttons:
            if sid == sync_id:
                btn.configure(fg_color=color, border_color="#FFFFFF")
            else:
                btn.configure(fg_color="#444444", border_color="#555555")

    def _refresh_presets(self):
        for widget in self.preset_buttons_frame.winfo_children():
            widget.destroy()

        presets = self.app.get_active_presets(self.current_type)
        for preset in presets[:10]:  # 最大10個
            text = preset.memo
            if preset.amount > 0:
                text += f" ¥{preset.amount:,}"
            btn = ctk.CTkButton(
                self.preset_buttons_frame, text=text,
                width=100, height=30,
                fg_color="#555555", hover_color="#777777",
                command=lambda p=preset: self._apply_preset(p),
            )
            btn.pack(side="left", padx=3, pady=2)

    def _apply_preset(self, preset):
        self.memo_entry.delete(0, "end")
        self.memo_entry.insert(0, preset.memo)
        if preset.amount > 0:
            self.amount_entry.delete(0, "end")
            self.amount_entry.insert(0, str(preset.amount))
        if preset.category_sync_id:
            self._select_category(preset.category_sync_id)
        # 使用回数を増やす
        preset.usage_count += 1
        preset.updated_at = _now_millis()

    def _submit(self):
        if not self.selected_category_sync_id:
            messagebox.showwarning("エラー", "カテゴリを選択してください")
            return

        amount_text = self.amount_entry.get().strip()
        if not amount_text:
            messagebox.showwarning("エラー", "金額を入力してください")
            return

        try:
            amount = int(amount_text)
        except ValueError:
            messagebox.showwarning("エラー", "金額は数値で入力してください")
            return

        memo = self.memo_entry.get().strip()

        daily = DailyData(
            date=self.current_date.isoformat(),
            amount=amount,
            memo=memo,
            type=self.current_type,
            category_sync_id=self.selected_category_sync_id,
        )
        self.app.data.daily_data.append(daily)
        self.app.save_data()

        # 入力欄クリア
        self.amount_entry.delete(0, "end")
        self.memo_entry.delete(0, "end")
        self.selected_category_sync_id = None
        self._refresh_categories()

        messagebox.showinfo("登録完了", f"¥{amount:,} を登録しました")

    def refresh(self):
        self._update_date_label()
        self._refresh_categories()
        self._refresh_presets()
