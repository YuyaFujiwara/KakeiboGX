"""ダイアログ共通処理 - 入力検証、モーダル設定、インラインエラー表示"""
# pyrefly: ignore [missing-import]
import customtkinter as ctk
from datetime import date

ERROR_COLOR = "#EF5350"


def parse_date(text, field_name="日付", allow_empty=False):
    """ISO形式(YYYY-MM-DD)の日付文字列を検証する。

    戻り値: (値, エラーメッセージ)
    値は正常時は "YYYY-MM-DD" 文字列、空許可で空欄の場合は None。
    """
    text = (text or "").strip()
    if not text:
        if allow_empty:
            return None, None
        return None, f"{field_name}を入力してください。"
    try:
        return date.fromisoformat(text).isoformat(), None
    except ValueError:
        return None, f"{field_name}は YYYY-MM-DD 形式で入力してください（例: {date.today().isoformat()}）。"


def parse_amount(text, field_name="金額"):
    """金額文字列を検証する。カンマと全角数字、円記号は許容する。"""
    text = (text or "").strip()
    if not text:
        return None, f"{field_name}を入力してください。"
    normalized = text.translate(str.maketrans("０１２３４５６７８９", "0123456789"))
    normalized = normalized.replace(",", "").replace("，", "").replace("¥", "").replace("円", "").strip()
    try:
        value = int(normalized)
    except ValueError:
        return None, f"{field_name}は数値で入力してください。"
    if value < 0:
        return None, f"{field_name}に負の値は指定できません。"
    return value, None


def parse_day_of_month(text, field_name="引落し日"):
    """1〜31の日付指定を検証する。"""
    text = (text or "").strip()
    if not text:
        return None, f"{field_name}を入力してください。"
    normalized = text.translate(str.maketrans("０１２３４５６７８９", "0123456789"))
    try:
        value = int(normalized)
    except ValueError:
        return None, f"{field_name}は数値で入力してください。"
    if not 1 <= value <= 31:
        return None, f"{field_name}は1〜31で入力してください。"
    return value, None


def make_error_label(parent):
    """インラインエラー表示用のラベルを作る。

    戻り値: (show, clear) の関数ペア。show(msg) でメッセージ表示、clear() で消去。
    """
    label = ctk.CTkLabel(parent, text="", text_color=ERROR_COLOR,
                         font=("", 12), wraplength=360, justify="left")
    label.pack(fill="x", padx=20, pady=(0, 2))

    def show(message):
        label.configure(text=message)

    def clear():
        label.configure(text="")

    return show, clear


def setup_modal(dialog, parent, title, width, height, on_close=None):
    """ダイアログをアプリ内モーダルとして設定し、親ウィンドウの中央に配置する。

    -topmost はデスクトップ全体の最前面に張り付いてしまうため使わず、
    transient + grab_set でアプリ内モーダルにする。
    """
    dialog.title(title)
    dialog.transient(parent.winfo_toplevel())
    dialog.resizable(False, True)
    dialog.minsize(width, 240)

    # 親の中央に配置
    dialog.update_idletasks()
    root = parent.winfo_toplevel()
    x = root.winfo_rootx() + (root.winfo_width() - width) // 2
    y = root.winfo_rooty() + (root.winfo_height() - height) // 2
    dialog.geometry(f"{width}x{height}+{max(x, 0)}+{max(y, 0)}")

    handler = on_close if on_close else dialog.destroy
    dialog.protocol("WM_DELETE_WINDOW", handler)
    dialog.bind("<Escape>", lambda _e: handler())
    dialog.grab_set()
    dialog.focus_set()
