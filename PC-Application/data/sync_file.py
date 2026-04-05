"""JSON同期ファイルの読み書き"""
import json
import os
from typing import Optional
from data.models import (
    SyncData, Category, DailyData, FixedCostSetting,
    Preset, QuotaSetting, _now_millis
)


def load_sync_file(path: str) -> SyncData:
    """JSONファイルを読み込んで SyncData を返す。ファイルが無ければ空の SyncData。"""
    if not os.path.exists(path):
        return SyncData()

    try:
        with open(path, "r", encoding="utf-8") as f:
            raw = json.load(f)
    except (json.JSONDecodeError, IOError):
        return SyncData()

    data = SyncData(
        version=raw.get("version", 1),
        last_modified=raw.get("lastModified", 0),
    )

    # Categories
    for c in raw.get("categories", []):
        data.categories.append(Category(
            sync_id=c.get("syncId", ""),
            name=c.get("name", ""),
            type=c.get("type", "EXPENSE"),
            icon_name=c.get("iconName", "ic_category_default"),
            color_code=c.get("colorCode", "808080"),
            display_order=c.get("displayOrder", 0),
            is_deleted=c.get("isDeleted", False),
            updated_at=c.get("updatedAt", 0),
        ))

    # DailyData
    for d in raw.get("dailyData", []):
        data.daily_data.append(DailyData(
            sync_id=d.get("syncId", ""),
            date=d.get("date", ""),
            amount=d.get("amount", 0),
            memo=d.get("memo", ""),
            type=d.get("type", "EXPENSE"),
            category_sync_id=d.get("categorySyncId", ""),
            fixed_cost_setting_sync_id=d.get("fixedCostSettingSyncId"),
            is_deleted=d.get("isDeleted", False),
            updated_at=d.get("updatedAt", 0),
        ))

    # FixedCostSettings
    for fc in raw.get("fixedCostSettings", []):
        data.fixed_cost_settings.append(FixedCostSetting(
            sync_id=fc.get("syncId", ""),
            name=fc.get("name", ""),
            amount=fc.get("amount", 0),
            type=fc.get("type", "EXPENSE"),
            category_sync_id=fc.get("categorySyncId", ""),
            frequency=fc.get("frequency", "MONTHLY"),
            day_of_month=fc.get("dayOfMonth", 1),
            day_of_week=fc.get("dayOfWeek", -1),
            start_date=fc.get("startDate", ""),
            end_date=fc.get("endDate"),
            day_off_option=fc.get("dayOffOption", "NONE"),
            last_inserted_to_daily_data=fc.get("lastInsertedToDailyData"),
            is_deleted=fc.get("isDeleted", False),
            updated_at=fc.get("updatedAt", 0),
        ))

    # Presets
    for p in raw.get("presets", []):
        data.presets.append(Preset(
            sync_id=p.get("syncId", ""),
            memo=p.get("memo", ""),
            amount=p.get("amount", 0),
            category_sync_id=p.get("categorySyncId"),
            type=p.get("type", "EXPENSE"),
            usage_count=p.get("usageCount", 0),
            display_order=p.get("displayOrder", 0),
            is_deleted=p.get("isDeleted", False),
            updated_at=p.get("updatedAt", 0),
        ))

    # QuotaSettings
    for q in raw.get("quotaSettings", []):
        data.quota_settings.append(QuotaSetting(
            sync_id=q.get("syncId", ""),
            category_sync_id=q.get("categorySyncId", ""),
            amount=q.get("amount", 0),
            is_monthly=q.get("isMonthly", True),
            is_deleted=q.get("isDeleted", False),
            updated_at=q.get("updatedAt", 0),
        ))

    return data


def save_sync_file(path: str, data: SyncData):
    """SyncData を JSON ファイルに書き出す。"""
    # フォルダがなければ作成
    os.makedirs(os.path.dirname(path), exist_ok=True)

    data.last_modified = _now_millis()

    raw = {
        "version": data.version,
        "lastModified": data.last_modified,
        "categories": [
            {
                "syncId": c.sync_id, "name": c.name, "type": c.type,
                "iconName": c.icon_name, "colorCode": c.color_code,
                "displayOrder": c.display_order, "isDeleted": c.is_deleted,
                "updatedAt": c.updated_at,
            } for c in data.categories
        ],
        "dailyData": [
            {
                "syncId": d.sync_id, "date": d.date, "amount": d.amount,
                "memo": d.memo, "type": d.type,
                "categorySyncId": d.category_sync_id,
                "fixedCostSettingSyncId": d.fixed_cost_setting_sync_id,
                "isDeleted": d.is_deleted, "updatedAt": d.updated_at,
            } for d in data.daily_data
        ],
        "fixedCostSettings": [
            {
                "syncId": fc.sync_id, "name": fc.name, "amount": fc.amount,
                "type": fc.type, "categorySyncId": fc.category_sync_id,
                "frequency": fc.frequency, "dayOfMonth": fc.day_of_month,
                "dayOfWeek": fc.day_of_week, "startDate": fc.start_date,
                "endDate": fc.end_date, "dayOffOption": fc.day_off_option,
                "lastInsertedToDailyData": fc.last_inserted_to_daily_data,
                "isDeleted": fc.is_deleted, "updatedAt": fc.updated_at,
            } for fc in data.fixed_cost_settings
        ],
        "presets": [
            {
                "syncId": p.sync_id, "memo": p.memo, "amount": p.amount,
                "categorySyncId": p.category_sync_id, "type": p.type,
                "usageCount": p.usage_count, "displayOrder": p.display_order,
                "isDeleted": p.is_deleted, "updatedAt": p.updated_at,
            } for p in data.presets
        ],
        "quotaSettings": [
            {
                "syncId": q.sync_id, "categorySyncId": q.category_sync_id,
                "amount": q.amount, "isMonthly": q.is_monthly,
                "isDeleted": q.is_deleted, "updatedAt": q.updated_at,
            } for q in data.quota_settings
        ],
    }

    with open(path, "w", encoding="utf-8") as f:
        json.dump(raw, f, ensure_ascii=False, indent=2)
