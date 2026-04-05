"""設定タブ - カテゴリ編集、プリセット管理、データ管理"""
import customtkinter as ctk
from tkinter import messagebox, filedialog
from data.models import Category, Preset, QuotaSetting, _now_millis, _new_sync_id
from data.sync_file import load_sync_file
import csv
import os


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

        # === データ管理 ===
        ctk.CTkLabel(main_frame, text="データ管理", font=("", 16, "bold")).pack(
            anchor="w", padx=5, pady=(20, 5))

        data_frame = ctk.CTkFrame(main_frame)
        data_frame.pack(fill="x", padx=5, pady=5)

        ctk.CTkButton(data_frame, text="CSVエクスポート",
                       command=self._export_csv).pack(side="left", padx=5, pady=5)
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
            for cat in categories:
                row = ctk.CTkFrame(self.cat_list_frame, fg_color="transparent")
                row.pack(fill="x", pady=1)

                try:
                    cat_color = f"#{cat.color_code}"
                    int(cat.color_code, 16)
                except ValueError:
                    cat_color = "#808080"

                indicator = ctk.CTkFrame(row, width=6, fg_color=cat_color, corner_radius=3)
                indicator.pack(side="left", fill="y", padx=(5, 8), pady=2)

                ctk.CTkLabel(row, text=cat.name, font=("", 12), anchor="w").pack(
                    side="left", padx=5, pady=3)

                ctk.CTkButton(row, text="削除", width=50, height=25,
                               fg_color="#555555", hover_color="#EF5350",
                               command=lambda c=cat: self._delete_category(c)).pack(
                    side="right", padx=5, pady=3)

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

        quotas = self.app.get_active_quota_settings()
        categories = self.app.get_active_categories("EXPENSE")

        for cat in categories:
            row = ctk.CTkFrame(self.quota_list_frame, fg_color="transparent")
            row.pack(fill="x", pady=1)

            ctk.CTkLabel(row, text=cat.name, font=("", 12), anchor="w",
                          width=100).pack(side="left", padx=5, pady=3)

            quota = next((q for q in quotas if q.category_sync_id == cat.sync_id), None)
            amount_text = f"¥{quota.amount:,}" if quota else "未設定"

            ctk.CTkLabel(row, text=amount_text, font=("", 12),
                          text_color="#4FC3F7" if quota else "#888888").pack(
                side="left", padx=10, pady=3)

    def _export_csv(self):
        path = filedialog.asksaveasfilename(
            defaultextension=".csv",
            filetypes=[("CSV files", "*.csv")],
            initialfile="household_data.csv"
        )
        if not path:
            return

        daily = self.app.get_active_daily_data()
        with open(path, "w", newline="", encoding="utf-8-sig") as f:
            writer = csv.writer(f)
            writer.writerow(["Date", "Category", "Type", "Amount", "Memo"])
            for d in sorted(daily, key=lambda x: x.date):
                cat = self.app.get_category_by_sync_id(d.category_sync_id)
                cat_name = cat.name if cat else "?"
                writer.writerow([d.date, cat_name, d.type, d.amount, d.memo])

        messagebox.showinfo("完了", f"CSVをエクスポートしました:\n{path}")

    def _reload_data(self):
        self.app.reload_data()
        messagebox.showinfo("完了", "データを再読み込みしました")
