"""データモデル - Android版のエンティティと対応"""
from dataclasses import dataclass, field
from datetime import datetime, date
from typing import Optional
import uuid


def _now_millis() -> int:
    return int(datetime.now().timestamp() * 1000)


def _new_sync_id() -> str:
    return str(uuid.uuid4())


@dataclass
class Category:
    sync_id: str = field(default_factory=_new_sync_id)
    name: str = ""
    type: str = "EXPENSE"  # "INCOME" or "EXPENSE"
    icon_name: str = "ic_category_default"
    color_code: str = "808080"
    display_order: int = 0
    is_deleted: bool = False
    updated_at: int = field(default_factory=_now_millis)


@dataclass
class DailyData:
    sync_id: str = field(default_factory=_new_sync_id)
    date: str = ""  # "yyyy-MM-dd"
    amount: int = 0
    memo: str = ""
    type: str = "EXPENSE"
    category_sync_id: str = ""
    fixed_cost_setting_sync_id: Optional[str] = None
    is_deleted: bool = False
    updated_at: int = field(default_factory=_now_millis)


@dataclass
class FixedCostSetting:
    sync_id: str = field(default_factory=_new_sync_id)
    name: str = ""
    amount: int = 0
    type: str = "EXPENSE"
    category_sync_id: str = ""
    frequency: str = "MONTHLY"
    day_of_month: int = 1
    day_of_week: int = -1
    start_date: str = ""
    end_date: Optional[str] = None
    day_off_option: str = "NONE"
    last_inserted_to_daily_data: Optional[str] = None
    is_deleted: bool = False
    updated_at: int = field(default_factory=_now_millis)


@dataclass
class Preset:
    sync_id: str = field(default_factory=_new_sync_id)
    memo: str = ""
    amount: int = 0
    category_sync_id: Optional[str] = None
    type: str = "EXPENSE"
    usage_count: int = 0
    display_order: int = 0
    is_deleted: bool = False
    updated_at: int = field(default_factory=_now_millis)


@dataclass
class QuotaSetting:
    sync_id: str = field(default_factory=_new_sync_id)
    category_sync_id: str = ""
    amount: int = 0
    is_monthly: bool = True
    is_deleted: bool = False
    updated_at: int = field(default_factory=_now_millis)


@dataclass
class SyncData:
    """JSON ファイル全体の構造"""
    version: int = 1
    last_modified: int = field(default_factory=_now_millis)
    categories: list = field(default_factory=list)
    daily_data: list = field(default_factory=list)
    fixed_cost_settings: list = field(default_factory=list)
    presets: list = field(default_factory=list)
    quota_settings: list = field(default_factory=list)
