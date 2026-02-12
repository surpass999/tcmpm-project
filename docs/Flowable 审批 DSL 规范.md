# Flowable 审批 DSL 规范（完整版）

## 目标



* 前端 100% 由 DSL 驱动

* 会签 / 退回 / 补正全部由流程配置决定

* 兼容 9 个菜单所有审批场景

## 一、DSL 总体结构



```
{

&#x20; "nodeKey": "EXPERT\_SELECT",

&#x20; "cap": "EXPERT\_SELECT",

&#x20; "actions": "selectExpert,submit",

&#x20; "roles": \["PROVINCE","HOSP","EXPERT","NATION"],

&#x20; "assign": {

&#x20;   "type": "STATIC\_ROLE | DYNAMIC\_USER | GROUP",

&#x20;   "source": "expertUsers | provinceRole"

&#x20; },

&#x20; "signRule": "ALL | ANY | MAJORITY | 2/3 | CUSTOM",

&#x20; "backStrategy": "TO\_START | TO\_PREV | TO\_ANY | TO\_ROLE",

&#x20; "reSubmit": "RESTART | RESUME",

&#x20; "bizStatus": "PRO\_AUDIT",

&#x20; "vars": {},

&#x20; "ui": {

&#x20;    "form": "expertSelect",

&#x20;    "readonly": false

&#x20; }

}
```

## 二、cap（节点能力）—— 最核心

### 审批类



| cap            | 说明   |
| -------------- | ---- |
| AUDIT          | 单人审批 |
| COUNTERSIGN    | 会签   |
| EXPERT\_SELECT | 选择专家 |
| FILL           | 填报   |
| MODIFY         | 补正   |
| CONFIRM        | 确认   |
| ARCHIVE        | 归档   |
| PUBLISH        | 发布   |

## 三、actions（全部按钮定义）

### 1. 流程控制



| key      | 含义 |
| -------- | -- |
| submit   | 提交 |
| agree    | 通过 |
| reject   | 拒绝 |
| back     | 退回 |
| cancel   | 撤回 |
| transfer | 转办 |
| delegate | 委派 |
| suspend  | 挂起 |
| resume   | 恢复 |

### 2. 会签专用



| key        |
| ---------- |
| addSign    |
| reduceSign |
| signAgree  |
| signReject |
| urge       |

### 3. 业务专用



| key          |
| ------------ |
| selectExpert |
| reSelect     |
| modify       |
| confirm      |
| archive      |
| toProject    |
| publish      |

## 四、assign（任务分配）

### 配置示例



```
"assign":{

&#x20;"type":"DYNAMIC\_USER",

&#x20;"source":"expertUsers"

}
```

### 类型说明



| type          | 说明    |
| ------------- | ----- |
| STATIC\_ROLE  | 固定角色  |
| DYNAMIC\_USER | 运行时用户 |
| GROUP         | 组织    |

## 五、signRule（会签规则）



| 规则       | 含义   |
| -------- | ---- |
| ALL      | 全部   |
| ANY      | 任一   |
| MAJORITY | 多数   |
| 2/3      | 三分之二 |
| CUSTOM   | 自定义  |

## 六、backStrategy（退回）



| 策略        |
| --------- |
| TO\_START |
| TO\_PREV  |
| TO\_ANY   |
| TO\_ROLE  |

## 七、vars（关键变量）

### 1. 专家相关



```
"vars":{

&#x20;"targetVar":"expertUsers",

&#x20;"min":1,

&#x20;"max":7

}
```

### 2. 补正



```
"vars":{

&#x20;"modifyFields":\["name","file"]

}
```

## 八、九菜单节点示例

### 1. 备案→省局



```
{

&#x20;"cap":"AUDIT",

&#x20;"actions":"agree,reject,back",

&#x20;"bizStatus":"PRO\_AUDIT"

}
```

### 2. 选择专家



```
{

&#x20;"cap":"EXPERT\_SELECT",

&#x20;"actions":"selectExpert,submit",

&#x20;"assign":{

&#x20;  "type":"DYNAMIC\_USER",

&#x20;  "source":"expertUsers"

&#x20;},

&#x20;"vars":{

&#x20;  "min":2,

&#x20;  "max":5

&#x20;}

}
```

### 3. 会签



```
{

&#x20;"cap":"COUNTERSIGN",

&#x20;"actions":"signAgree,signReject,urge",

&#x20;"assign":{

&#x20;  "type":"DYNAMIC\_USER",

&#x20;  "source":"expertUsers"

&#x20;},

&#x20;"signRule":"MAJORITY"

}
```

### 4. 国家审核



```
{

&#x20;"cap":"AUDIT",

&#x20;"actions":"agree,reject"

}
```

## 九、前端解释规范



1. 按钮 = actions



```
buttons = actions.split(',')
```



1. 会签不用判断，只显示 signAgree/signReject

2. 选专家



```
if cap==EXPERT\_SELECT:

&#x20; 打开专家选择器
```

## 十、与 Flowable 的映射



* DSL → extensionElements

* expertUsers → 多实例

* signRule → 完成条件

> （注：文档部分内容可能由 AI 生成）