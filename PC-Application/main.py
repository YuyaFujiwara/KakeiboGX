"""家計簿GX - PC版 (Python customtkinter)"""
import sys
import os

# プロジェクトルートをパスに追加
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import customtkinter as ctk
import config

def main():
    ctk.set_appearance_mode(config.APPEARANCE_MODE)
    ctk.set_default_color_theme(config.COLOR_THEME)

    from ui.app import App
    app = App()
    app.mainloop()

if __name__ == "__main__":
    main()
