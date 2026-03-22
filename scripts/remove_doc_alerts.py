#!/usr/bin/env python3
import re
import os
from pathlib import Path

views_dir = Path("web-ui/apps/web-antd/src/views")
count = 0

for vue_file in views_dir.rglob("*.vue"):
    try:
        content = vue_file.read_text(encoding='utf-8')
        original = content
        
        # 1. 移除 <template #doc>...</template> 块
        pattern = r'\s*<template #doc>.*?</template>\n?'
        content = re.sub(pattern, '\n', content, flags=re.DOTALL)
        
        # 2. 清理 import 中的 DocAlert
        # 匹配 import { ..., DocAlert, ... } from '...'
        content = re.sub(
            r",\s*DocAlert(?=[,\s}])",
            "",
            content
        )
        content = re.sub(
            r"DocAlert,\s*",
            "",
            content
        )
        # 清理空的 import {  } 或 {DocAlert}
        content = re.sub(
            r"import\s*\{\s*\}\s*from\s+['\"]@vben/common-ui['\"]",
            "",
            content
        )
        # 清理只有 DocAlert 的 import
        content = re.sub(
            r"import\s*\{\s*DocAlert\s*\}\s*from\s+['\"]@vben/common-ui['\"]",
            "",
            content
        )
        
        if content != original:
            vue_file.write_text(content, encoding='utf-8')
            count += 1
            print(f"Updated: {vue_file}")
    except Exception as e:
        print(f"Error processing {vue_file}: {e}")

print(f"\nTotal files updated: {count}")
