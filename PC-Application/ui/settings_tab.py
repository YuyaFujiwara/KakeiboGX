"""設定タブ - カテゴリ編集、プリセット管理、データ管理"""
# pyrefly: ignore [missing-import]
import customtkinter as ctk
import tkinter as tk
from tkinter import messagebox, filedialog
from data.models import Category, Preset, QuotaSetting, _now_millis, _new_sync_id
from data.sync_file import load_sync_file
from ui.dialog_utils import (parse_date, parse_amount, parse_day_of_month,
                             make_error_label, setup_modal, theme_row_colors)
from datetime import date
import csv
import os

# カテゴリ未登録時にオプションメニューへ出すプレースホルダ
NO_CATEGORY = "(カテゴリ未登録)"


class SettingsTab:
    def __init__(self, parent, app):
        self.parent = parent
        self.app = app
        self._build_ui()

    def _build_ui(self):
        # スクロール可能なメインフレーム
        main_frame = ctk.CTkScrollableFrame(self.parent)
        main_frame.pack(fill="both", expand=True, padx=10, pady=10)

        # === カテゴリ管理 ===
        ctk.CTkLabel(main_frame, text="カテゴリ管理", font=("", 16, "bold")).pack(
            anchor="w", padx=5, pady=(10, 5))

        cat_btn_frame = ctk.CTkFrame(main_frame)
        cat_btn_frame.pack(fill="x", padx=5, pady=5)

        ctk.CTkButton(cat_btn_frame, text="カテゴリを追加",
                       command=self._add_category).pack(side="left", padx=5, pady=5)

        self.cat_list_frame = ctk.CTkFrame(main_frame)
        self.cat_list_frame.pack(fill="x", padx=5, pady=5)

        # === プリセット管理 ===
        ctk.CTkLabel(main_frame, text="プリセット管理", font=("", 16, "bold")).pack(
            anchor="w", padx=5, pady=(20, 5))

        preset_btn_frame = ctk.CTkFrame(main_frame)
        preset_btn_frame.pack(fill="x", padx=5, pady=5)

        ctk.CTkButton(preset_btn_frame, text="プリセットを追加",
                       command=self._add_preset).pack(side="left", padx=5, pady=5)

        self.preset_list_frame = ctk.CTkFrame(main_frame)
        self.preset_list_frame.pack(fill="x", padx=5, pady=5)

        # === 予算設定 ===
        ctk.CTkLabel(main_frame, text="予算(クォータ)設定", font=("", 16, "bold")).pack(
            anchor="w", padx=5, pady=(20, 5))

        self.quota_list_frame = ctk.CTkFrame(main_frame)
        self.quota_list_frame.pack(fill="x", padx=5, pady=5)

        # === 定期収支設定 ===
        ctk.CTkLabel(main_frame, text="定期収支(固定費)設定", font=("", 16, "bold")).pack(
            anchor="w", padx=5, pady=(20, 5))

        fc_btn_frame = ctk.CTkFrame(main_frame)
        fc_btn_frame.pack(fill="x", padx=5, pady=5)

        ctk.CTkButton(fc_btn_frame, text="定期設定を追加",
                       command=self._add_fixed_cost).pack(side="left", padx=5, pady=5)

        self.fc_list_frame = ctk.CTkFrame(main_frame)
        self.fc_list_frame.pack(fill="x", padx=5, pady=5)

        # === データ管理 ===
        ctk.CTkLabel(main_frame, text="データ管理", font=("", 16, "bold")).pack(
            anchor="w", padx=5, pady=(20, 5))

        data_frame = ctk.CTkFrame(main_frame)
        data_frame.pack(fill="x", padx=5, pady=5)

        ctk.CTkButton(data_frame, text="ローカルへCSVエクスポート",
                       command=self._export_csv_local).pack(side="left", padx=5, pady=5)
        ctk.CTkButton(data_frame, text="DriveへCSVエクスポート",
                       command=self._export_csv_drive).pack(side="left", padx=5, pady=5)
        ctk.CTkButton(data_frame, text="JSONファイル再読み込み",
                       command=self._reload_data).pack(side="left", padx=5, pady=5)

        # === 同期ファイルパス表示 ===
        ctk.CTkLabel(main_frame, text="同期ファイル", font=("", 16, "bold")).pack(
            anchor="w", padx=5, pady=(20, 5))

        path_frame = ctk.CTkFrame(main_frame)
        path_frame.pack(fill="x", padx=5, pady=5)

        ctk.CTkLabel(path_frame, text=self.app.sync_file_path,
                       font=("", 11), text_color="#AAAAAA").pack(side="left", padx=10, pady=8)

        exists = os.path.exists(self.app.sync_file_path)
        status = "✓ ファイルあり" if exists else "✗ ファイルなし"
        color = "#4FC3F7" if exists else "#EF5350"
        self.file_status = ctk.CTkLabel(path_frame, text=status,
                                          font=("", 11), text_color=color)
        self.file_status.pack(side="right", padx=10, pady=8)

    def refresh(self):
        self._refresh_categories()
        self._refresh_presets()
        self._refresh_quotas()
        self._refresh_fixed_costs()

        exists = os.path.exists(self.app.sync_file_path)
        status = "✓ ファイルあり" if exists else "✗ ファイルなし"
        color = "#4FC3F7" if exists else "#EF5350"
        self.file_status.configure(text=status, text_color=color)

    def _refresh_categories(self):
        for widget in self.cat_list_frame.winfo_children():
            widget.destroy()

        for cat_type in ["EXPENSE", "INCOME"]:
            type_label = "支出カテゴリ" if cat_type == "EXPENSE" else "収入カテゴリ"
            ctk.CTkLabel(self.cat_list_frame, text=type_label,
                          font=("", 13, "bold")).pack(anchor="w", padx=5, pady=(8, 2))

            categories = self.app.get_active_categories(cat_type)
            row_bg, row_fg = theme_row_colors(self.cat_list_frame)
            for cat in categories:
                row = tk.Frame(self.cat_list_frame, bg=row_bg)
                row.pack(fill="x", pady=1)

                try:
                    cat_color = f"#{cat.color_code}"
                    int(cat.color_code, 16)
                except ValueError:
                    cat_color = "#808080"

                tk.Frame(row, width=6, height=16, bg=cat_color).pack(side="left", padx=(5, 8))

                tk.Label(row, text=cat.name, font=("", 12), anchor="w",
                          bg=row_bg, fg=row_fg).pack(side="left", padx=5, pady=2)

                tk.Button(row, text="削除", font=("", 10), width=5,
                           bg="#555555", fg="#FFFFFF", activebackground="#EF5350",
                           activeforeground="#FFFFFF", relief="flat", bd=0, cursor="hand2",
                           command=lambda c=cat: self._delete_category(c)).pack(
                    side="right", padx=5, pady=2)

    def _add_category(self):
        dialog = ctk.CTkInputDialog(text="カテゴリ名:", title="カテゴリ追加")
        name = dialog.get_input()
        if not name:
            return

        cat = Category(
            name=name,
            type="EXPENSE",
            display_order=len(self.app.get_active_categories("EXPENSE")) + 1,
        )
        self.app.data.categories.append(cat)
        self.app.save_data()
        self._refresh_categories()

    def _delete_category(self, cat):
        if messagebox.askyesno("確認", f"「{cat.name}」カテゴリを削除しますか？"):
            cat.is_deleted = True
            cat.updated_at = _now_millis()
            self.app.save_data()
            self._refresh_categories()

    def _refresh_presets(self):
        for widget in self.preset_list_frame.winfo_children():
            widget.destroy()

        presets = self.app.get_active_presets()
        for p in presets:
            row = ctk.CTkFrame(self.preset_list_frame, fg_color="transparent")
            row.pack(fill="x", pady=1)

            text = p.memo
            if p.amount > 0:
                text += f" ¥{p.amount:,}"
            ctk.CTkLabel(row, text=text, font=("", 12), anchor="w").pack(
                side="left", padx=10, pady=3)

            ctk.CTkButton(row, text="削除", width=50, height=25,
                           fg_color="#555555", hover_color="#EF5350",
                           command=lambda pr=p: self._delete_preset(pr)).pack(
                side="right", padx=5, pady=3)

    def _add_preset(self):
        dialog = ctk.CTkInputDialog(text="プリセット名 (例: 昼飯:500)", title="プリセット追加")
        text = dialog.get_input()
        if not text:
            return

        # "メモ:金額" 形式をパース
        if ":" in text:
            parts = text.split(":", 1)
            memo = parts[0].strip()
            try:
                amount = int(parts[1].strip())
            except ValueError:
                amount = 0
        else:
            memo = text.strip()
            amount = 0

        preset = Preset(memo=memo, amount=amount)
        self.app.data.presets.append(preset)
        self.app.save_data()
        self._refresh_presets()

    def _delete_preset(self, preset):
        preset.is_deleted = True
        preset.updated_at = _now_millis()
        self.app.save_data()
        self._refresh_presets()

    def _refresh_quotas(self):
        for widget in self.quota_list_frame.winfo_children():
            widget.destroy()

        categories = self.app.get_active_categories("EXPENSE")

        row_bg, row_fg = theme_row_colors(self.quota_list_frame)
        for cat in categories:
            row = tk.Frame(self.quota_list_frame, bg=row_bg)
            row.pack(fill="x", pady=1)

            tk.Label(row, text=cat.name, font=("", 12), anchor="w", width=13,
                      bg=row_bg, fg=row_fg).pack(side="left", padx=5, pady=3)

            quota = self.app.get_quota_for_category(cat.sync_id)
            amount_text = f"¥{quota.amount:,}" if quota else "未設定"

            tk.Label(row, text=amount_text, font=("", 12), bg=row_bg,
                      fg="#4FC3F7" if quota else "#888888").pack(
                side="left", padx=10, pady=3)

    def _show_month_selection(self, callback):
        daily = self.app.get_active_daily_data()
        months = sorted(list(set(d.date[:7] for d in daily if len(d.date) >= 7)), reverse=True)
        options = ["すべて"] + months

        dialog = ctk.CTkToplevel(self.parent)
        dialog.title("出力する月を選択")
        dialog.geometry("300x150")
        dialog.attributes("-topmost", True)
        dialog.grab_set()

        ctk.CTkLabel(dialog, text="対象の月を選択してください:").pack(pady=(15, 5))

        selected_var = ctk.StringVar(value=options[0])
        opt_menu = ctk.CTkOptionMenu(dialog, values=options, variable=selected_var)
        opt_menu.pack(pady=5)

        def on_ok():
            val = selected_var.get()
            target = None if val == "すべて" else val
            dialog.destroy()
            callback(target)

        def on_cancel():
            dialog.destroy()

        btn_frame = ctk.CTkFrame(dialog, fg_color="transparent")
        btn_frame.pack(pady=(10, 5))
        ctk.CTkButton(btn_frame, text="OK", width=80, command=on_ok).pack(side="left", padx=10)
        ctk.CTkButton(btn_frame, text="キャンセル", width=80, fg_color="#555555", hover_color="#333333", command=on_cancel).pack(side="left", padx=10)

    def _export_csv_local(self):
        def on_selected(target_month):
            filename = f"household_data_{target_month}.csv" if target_month else "household_data.csv"
            path = filedialog.asksaveasfilename(
                defaultextension=".csv",
                filetypes=[("CSV files", "*.csv")],
                initialfile=filename
            )
            if not path:
                return
            self._write_csv(path, target_month)
            messagebox.showinfo("完了", f"CSVをエクスポートしました:\n{path}")
            
        self._show_month_selection(on_selected)

    def _export_csv_drive(self):
        def on_selected(target_month):
            drive_dir = os.path.dirname(self.app.sync_file_path)
            if not os.path.exists(drive_dir):
                messagebox.showerror("エラー", "Driveの同期フォルダが見つかりません。")
                return
                
            filename = f"household_data_{target_month}.csv" if target_month else "household_data.csv"
            path = os.path.join(drive_dir, filename)
            try:
                self._write_csv(path, target_month)
                messagebox.showinfo("完了", f"DriveにCSVを直接エクスポートしました:\n{path}")
            except Exception as e:
                messagebox.showerror("エラー", f"書き込みに失敗しました:\n{e}")
                
        self._show_month_selection(on_selected)

    def _write_csv(self, path, target_month=None):
        daily = self.app.get_active_daily_data()
        if target_month:
            daily = [d for d in daily if d.date.startswith(target_month)]
            
        with open(path, "w", newline="", encoding="utf-8-sig") as f:
            writer = csv.writer(f)
            writer.writerow(["Date", "Category", "Type", "Amount", "Memo", "PaymentMethod"])
            for d in sorted(daily, key=lambda x: x.date):
                cat = self.app.get_category_by_sync_id(d.category_sync_id)
                cat_name = cat.name if cat else "?"
                payment = d.payment_method if d.payment_method else ""
                writer.writerow([d.date, cat_name, d.type, d.amount, d.memo, payment])

    def _reload_data(self):
        self.app.reload_data()
        messagebox.showinfo("完了", "データを再読み込みしました")

    def _refresh_fixed_costs(self):
        for widget in self.fc_list_frame.winfo_children():
            widget.destroy()

        fc_settings = [fc for fc in self.app.data.fixed_cost_settings if not fc.is_deleted]
        if not fc_settings:
            ctk.CTkLabel(self.fc_list_frame, text="定期設定はまだありません",
                          text_color="#888888", font=("", 12), anchor="w").pack(
                fill="x", padx=10, pady=6)
            return

        today = date.today()
        row_bg, row_fg = theme_row_colors(self.fc_list_frame)
        for fc in fc_settings:
            row = tk.Frame(self.fc_list_frame, bg=row_bg)
            row.pack(fill="x", pady=1)

            cat = self.app.get_category_by_sync_id(fc.category_sync_id)
            cat_name = cat.name if cat else "カテゴリ未設定"
            name = fc.name if fc.name else "名称未設定"

            # 期間（終了日を過ぎたものは失効として明示する）
            start = fc.start_date if fc.start_date else "?"
            end = fc.end_date if fc.end_date else "無期限"
            expired = False
            if fc.end_date:
                try:
                    expired = date.fromisoformat(fc.end_date) < today
                except ValueError:
                    pass

            # 収入/支出を符号と色で区別する（カレンダータブと同じ表現）
            is_income = fc.type == "INCOME"
            sign = "+" if is_income else "-"
            amount_color = "#4FC3F7" if is_income else "#EF5350"
            if expired:
                amount_color = "#888888"

            text_frame = tk.Frame(row, bg=row_bg)
            text_frame.pack(side="left", fill="x", expand=True, padx=10, pady=3)

            title = f"[{cat_name}] {name}"
            if expired:
                title += "（終了）"
            tk.Label(text_frame, text=title, font=("", 12), anchor="w",
                      bg=row_bg, fg="#888888" if expired else row_fg).pack(fill="x")
            tk.Label(text_frame, text=f"毎月{fc.day_of_month}日 / 期間: {start} 〜 {end}",
                      font=("", 11), bg=row_bg, fg="#888888", anchor="w").pack(fill="x")

            btn_frame = tk.Frame(row, bg=row_bg)
            btn_frame.pack(side="right", padx=5, pady=3)

            tk.Label(btn_frame, text=f"{sign}¥{fc.amount:,}", width=12, anchor="e",
                      font=("", 12, "bold"), bg=row_bg, fg=amount_color).pack(
                side="left", padx=(0, 10))

            tk.Button(btn_frame, text="編集", font=("", 10), width=4,
                       bg="#555555", fg="#FFFFFF", activebackground="#4FC3F7",
                       activeforeground="#FFFFFF", relief="flat", bd=0, cursor="hand2",
                       command=lambda f=fc: self._edit_fixed_cost(f)).pack(side="left", padx=2)

            tk.Button(btn_frame, text="削除", font=("", 10), width=4,
                       bg="#555555", fg="#FFFFFF", activebackground="#EF5350",
                       activeforeground="#FFFFFF", relief="flat", bd=0, cursor="hand2",
                       command=lambda f=fc: self._delete_fixed_cost(f)).pack(side="left", padx=2)

    def _find_fixed_cost(self, sync_id):
        """syncIdで定期設定を引き直す（バックグラウンド再読み込み対策）"""
        for fc in self.app.data.fixed_cost_settings:
            if fc.sync_id == sync_id:
                return fc
        return None

    def _delete_fixed_cost(self, fc):
        sync_id = fc.sync_id
        label = fc.name if fc.name else "名称未設定"
        if messagebox.askyesno("確認", f"「{label}」の定期設定を削除しますか？"):
            target = self._find_fixed_cost(sync_id)
            if target is None:
                self._refresh_fixed_costs()
                return
            target.is_deleted = True
            target.updated_at = _now_millis()
            self.app.save_data()
            self._refresh_fixed_costs()

    def _add_fixed_cost(self):
        from data.models import FixedCostSetting
        # データへの追加は保存時に行う。ここで append すると、ダイアログを×で
        # 閉じられた場合に空の設定が残ってしまう。
        fc = FixedCostSetting(
            sync_id=_new_sync_id(),
            name="",
            amount=0,
            type="EXPENSE",
            category_sync_id="",
            frequency="MONTHLY",
            day_of_month=1,
            start_date=date.today().isoformat()
        )
        self._edit_fixed_cost(fc, is_new=True)

    def _edit_fixed_cost(self, fc, is_new=False):
        sync_id = fc.sync_id
        dialog = ctk.CTkToplevel(self.parent)
        setup_modal(dialog, self.parent, "定期設定の追加" if is_new else "定期設定の編集", 460, 600)

        body = ctk.CTkScrollableFrame(dialog, fg_color="transparent")
        body.pack(fill="both", expand=True, padx=5, pady=5)

        # 名称
        ctk.CTkLabel(body, text="メモ/名称:", anchor="w").pack(fill="x", padx=20, pady=(10, 0))
        name_var = ctk.StringVar(value=fc.name)
        name_entry = ctk.CTkEntry(body, textvariable=name_var, placeholder_text="家賃、電気代など")
        name_entry.pack(pady=5, padx=20, fill="x")

        # 金額（0は空欄にして、消してから入力する手間をなくす）
        ctk.CTkLabel(body, text="金額:", anchor="w").pack(fill="x", padx=20, pady=(10, 0))
        amount_var = ctk.StringVar(value=str(fc.amount) if fc.amount else "")
        ctk.CTkEntry(body, textvariable=amount_var, placeholder_text="50000").pack(
            pady=5, padx=20, fill="x")

        # タイプ
        ctk.CTkLabel(body, text="タイプ:", anchor="w").pack(fill="x", padx=20, pady=(10, 0))
        type_frame = ctk.CTkFrame(body, fg_color="transparent")
        type_frame.pack(fill="x", padx=20, pady=5)
        type_var = ctk.StringVar(value=fc.type)
        ctk.CTkRadioButton(type_frame, text="支出", variable=type_var, value="EXPENSE").pack(
            side="left", padx=(0, 20))
        ctk.CTkRadioButton(type_frame, text="収入", variable=type_var, value="INCOME").pack(side="left")

        # 支払方法（支出のときだけ表示する。収入では保存時に None になるため）
        pm_var = ctk.StringVar(value=fc.payment_method if fc.payment_method else "CARD")
        pm_label = ctk.CTkLabel(body, text="支払方法:", anchor="w")
        pm_frame = ctk.CTkFrame(body, fg_color="transparent")
        ctk.CTkRadioButton(pm_frame, text="現金", variable=pm_var, value="CASH").pack(
            side="left", padx=(0, 20))
        ctk.CTkRadioButton(pm_frame, text="カード", variable=pm_var, value="CARD").pack(side="left")

        # カテゴリ
        cat_label = ctk.CTkLabel(body, text="カテゴリ:", anchor="w")
        cat_label.pack(fill="x", padx=20, pady=(10, 0))
        cat_var = ctk.StringVar()
        opt_menu = ctk.CTkOptionMenu(body, variable=cat_var, values=[NO_CATEGORY])
        opt_menu.pack(pady=5, padx=20, fill="x")

        # 引落し日
        ctk.CTkLabel(body, text="引落し日 (1-31):", anchor="w").pack(fill="x", padx=20, pady=(10, 0))
        day_var = ctk.StringVar(value=str(fc.day_of_month))
        ctk.CTkEntry(body, textvariable=day_var).pack(pady=5, padx=20, fill="x")

        # 開始日 / 終了日
        ctk.CTkLabel(body, text="開始日 (YYYY-MM-DD):", anchor="w").pack(fill="x", padx=20, pady=(10, 0))
        start_var = ctk.StringVar(value=fc.start_date)
        ctk.CTkEntry(body, textvariable=start_var, placeholder_text="2026-01-01").pack(
            pady=5, padx=20, fill="x")

        ctk.CTkLabel(body, text="終了日 (空欄なら無期限):", anchor="w").pack(fill="x", padx=20, pady=(10, 0))
        end_var = ctk.StringVar(value=fc.end_date if fc.end_date else "")
        ctk.CTkEntry(body, textvariable=end_var, placeholder_text="空欄可").pack(
            pady=5, padx=20, fill="x")

        show_error, clear_error = make_error_label(body)

        def update_categories(*_args):
            t = type_var.get()

            # 収入のときは支払方法を隠す（効果のないコントロールを見せない）
            if t == "EXPENSE":
                pm_label.pack(fill="x", padx=20, pady=(10, 0), before=cat_label)
                pm_frame.pack(fill="x", padx=20, pady=5, before=cat_label)
            else:
                pm_label.pack_forget()
                pm_frame.pack_forget()

            cats = self.app.get_active_categories(t)
            cat_names = [c.name for c in cats]
            if not cat_names:
                cat_names = [NO_CATEGORY]

            opt_menu.configure(values=cat_names)
            current_cat = self.app.get_category_by_sync_id(fc.category_sync_id)
            if current_cat and current_cat.type == t:
                cat_var.set(current_cat.name)
            else:
                cat_var.set(cat_names[0])

        type_var.trace_add("write", update_categories)
        update_categories()

        def on_save():
            clear_error()

            name_val = name_var.get().strip()
            if not name_val:
                show_error("メモ/名称を入力してください。")
                name_entry.focus_set()
                return

            amount_val, err = parse_amount(amount_var.get(), "金額")
            if err:
                show_error(err)
                return

            type_val = type_var.get()
            cats = self.app.get_active_categories(type_val)
            sel_cat_name = cat_var.get()
            category_sync_id = None
            for c in cats:
                if c.name == sel_cat_name:
                    category_sync_id = c.sync_id
                    break
            if category_sync_id is None:
                show_error("カテゴリを選択してください。このタイプのカテゴリが未登録の場合は、"
                            "先に「カテゴリ管理」で追加してください。")
                return

            day_val, err = parse_day_of_month(day_var.get())
            if err:
                show_error(err)
                return

            start_val, err = parse_date(start_var.get(), "開始日")
            if err:
                show_error(err)
                return

            end_val, err = parse_date(end_var.get(), "終了日", allow_empty=True)
            if err:
                show_error(err)
                return

            if end_val and end_val < start_val:
                show_error("終了日は開始日以降の日付にしてください。")
                return

            if is_new:
                target = fc
            else:
                target = self._find_fixed_cost(sync_id)
                if target is None:
                    show_error("この設定は他の端末で変更または削除されました。画面を更新します。")
                    self._refresh_fixed_costs()
                    dialog.after(1200, dialog.destroy)
                    return

            target.name = name_val
            target.amount = amount_val
            target.type = type_val
            target.payment_method = pm_var.get() if type_val == "EXPENSE" else None
            target.category_sync_id = category_sync_id
            target.day_of_month = day_val
            target.start_date = start_val
            target.end_date = end_val
            target.updated_at = _now_millis()

            if is_new:
                self.app.data.fixed_cost_settings.append(target)

            self.app.save_data()
            self._refresh_fixed_costs()
            dialog.destroy()

        btn_frame = ctk.CTkFrame(dialog, fg_color="transparent")
        btn_frame.pack(pady=(5, 15))
        ctk.CTkButton(btn_frame, text="保存", width=100, command=on_save).pack(side="left", padx=10)
        ctk.CTkButton(btn_frame, text="キャンセル", width=100, fg_color="#555555",
                       hover_color="#333333", command=dialog.destroy).pack(side="left", padx=10)

        dialog.bind("<Return>", lambda _e: on_save())
        name_entry.focus_set()
