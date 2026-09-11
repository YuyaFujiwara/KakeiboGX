"""ダイアログ共通処理 - 入力検証、モーダル設定、インラインエラー表示"""
# pyrefly: ignore [missing-import]
import customtkinter as ctk
from customtkinter import ThemeManager
from datetime import date

ERROR_COLOR = "#EF5350"


def theme_row_colors(parent):
    """素のtkウィジェットに CustomTkinter のテーマ色を合わせるための色を返す。

    一覧の行は1行あたり6〜8個のウィジェットになるため、CTkウィジェット
    （1個あたり1.4〜2.2ms）で組むと数百msかかる。見た目はテーマ色を引き継いだ
    まま素のtkで描画するために、親フレームの実際の背景色と文字色を解決する。

    戻り値: (背景色, 文字色)
    """
    fg_color = parent.cget("fg_color")
    if fg_color == "transparent":
        fg_color = ThemeManager.theme["CTkFrame"]["fg_color"]
    return (parent._apply_appearance_mode(fg_color),
            parent._apply_appearance_mode(ThemeManager.theme["CTkLabel"]["text_color"]))


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
