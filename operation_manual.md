# 飞书表格助手操作文档

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 快速开始](#2-快速开始)
- [3. 核心概念](#3-核心概念)
- [4. 注解方式使用（实体类）](#4-注解方式使用实体类)
- [5. Map 配置方式使用](#5-map-配置方式使用)
- [6. 高级特性](#6-高级特性)
- [7. 实际应用场景](#7-实际应用场景)
- [8. API 参考](#8-api-参考)
- [9. 最佳实践](#9-最佳实践)
- [10. 常见问题（FAQ）](#10-常见问题faq)

---

## 1. 项目概述

### 1.1 项目简介

`feishu-table-helper` 是一个简化飞书表格操作的 Java 库。通过使用注解或 Map 配置，开发者可以轻松地将 Java 实体类映射到飞书表格，实现表格的自动创建、数据读取和写入操作，大大简化了与飞书表格 API 的交互。

### 1.2 核心能力

- **注解驱动**：使用 `@TableProperty` 和 `@TableConf` 注解将实体类字段映射到表格列
- **自动创建表格**：根据实体类结构自动创建飞书表格和设置表头
- **数据读取**：从飞书表格读取数据并映射到实体类对象
- **数据写入**：将实体类对象写入飞书表格，支持新增和更新操作（Upsert）
- **灵活配置**：支持自定义表格样式、单元格格式、下拉选项等
- **Map 方式**：无需定义实体类，直接使用 Map 配置操作表格
- **多层级表头**：支持复杂的多层级表头结构
- **分组表格**：支持分组字段，实现分组数据的创建和读取

### 1.3 适用场景

- **数据同步**：将业务系统的数据同步到飞书表格
- **报表生成**：自动生成和更新飞书表格报表
- **数据采集**：通过飞书表格收集数据并回读到系统
- **数据展示**：将系统数据以表格形式展示给非技术人员
- **协作办公**：团队成员通过飞书表格协作编辑数据

### 1.4 版本信息

- **当前版本**：0.0.5
- **最低 Java 版本**：Java 8
- **主要依赖**：
  - 飞书开放平台 SDK (oapi-sdk) v2.4.21
  - OkHttp v4.12.0
  - Gson v2.8.9

---

## 2. 快速开始

### 2.1 环境准备

#### Maven 依赖

```xml
<dependency>
    <groupId>cn.isliu</groupId>
    <artifactId>feishu-table-helper</artifactId>
    <version>0.0.5</version>
</dependency>
```

#### Gradle 依赖

```gradle
implementation 'cn.isliu:feishu-table-helper:0.0.5'
```

### 2.2 初始化配置

在使用飞书表格助手之前，需要先初始化飞书客户端。你需要从飞书开放平台获取应用的 `App ID` 和 `App Secret`。

```java
import cn.isliu.core.client.FsClient;

// 初始化配置
try (FsClient fsClient = FsClient.getInstance()) {
    fsClient.initializeClient("your_app_id", "your_app_secret");
    
    // 后续的表格操作都在这个 try-with-resources 块中进行
    // 或者在整个应用生命周期中保持 FsClient 实例
}
```

**重要提示**：
- `FsClient.getInstance()` 返回的是单例实例
- 建议使用 try-with-resources 确保资源正确释放
- 如果需要在应用生命周期中保持连接，可以在应用启动时初始化一次

### 2.3 第一个示例

让我们通过一个简单的员工信息管理示例来快速上手：

#### 步骤 1：创建实体类

```java
import cn.isliu.core.BaseEntity;
import cn.isliu.core.annotation.TableConf;
import cn.isliu.core.annotation.TableProperty;

@TableConf(headLine = 2, titleRow = 1, enableDesc = true)
public class Employee extends BaseEntity {

    @TableProperty(value = "员工编号", order = 0, desc = "员工编号不超过20个字符")
    private String employeeId;

    @TableProperty(value = "姓名", order = 1, desc = "员工姓名不超过20个字符")
    private String name;

    @TableProperty(value = "部门", order = 2, desc = "员工部门")
    private String department;

    @TableProperty(value = "邮箱", order = 3, desc = "员工邮箱")
    private String email;

    // getters and setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

#### 步骤 2：创建表格

```java
import cn.isliu.FsHelper;

// 假设你已经初始化了 FsClient
String spreadsheetToken = "your_spreadsheet_token"; // 电子表格Token
String sheetId = FsHelper.create("员工表", spreadsheetToken, Employee.class);
System.out.println("创建的工作表ID: " + sheetId);
```

#### 步骤 3：写入数据

```java
List<Employee> employees = new ArrayList<>();
Employee emp = new Employee();
emp.setEmployeeId("E001");
emp.setName("张三");
emp.setDepartment("技术部");
emp.setEmail("zhangsan@company.com");
employees.add(emp);

Employee emp2 = new Employee();
emp2.setEmployeeId("E002");
emp2.setName("李四");
emp2.setDepartment("产品部");
emp2.setEmail("lisi@company.com");
employees.add(emp2);

FsHelper.write(sheetId, spreadsheetToken, employees);
```

#### 步骤 4：读取数据

```java
List<Employee> employees = FsHelper.read(sheetId, spreadsheetToken, Employee.class);
employees.forEach(emp -> {
    System.out.println(emp.getName() + " - " + emp.getEmail());
    System.out.println("唯一ID: " + emp.getUniqueId());
    System.out.println("行号: " + emp.getRow());
});
```

---

## 3. 核心概念

### 3.1 两种使用方式

#### 方式一：注解方式（实体类）

使用 `@TableConf` 和 `@TableProperty` 注解在实体类上定义表格结构，适用于：
- 表格结构相对固定
- 需要类型安全
- 需要 IDE 自动补全

**优点**：
- 类型安全
- IDE 支持好
- 代码可读性强

**缺点**：
- 需要定义实体类
- 结构变更需要重新编译

#### 方式二：Map 配置方式

使用 `MapSheetConfig`、`MapTableConfig` 和 `MapFieldDefinition` 配置表格，适用于：
- 动态字段
- 表格结构经常变化
- 不需要定义实体类

**优点**：
- 灵活，支持动态配置
- 无需定义实体类
- 适合配置驱动的场景

**缺点**：
- 类型安全性较低
- 需要手动处理类型转换

### 3.2 重要术语

#### spreadsheetToken（电子表格 Token）

飞书电子表格的唯一标识符。你可以在飞书表格的 URL 中找到，例如：
```
https://example.feishu.cn/sheets/shtcnxxxxxxxxxxxxxxxxx
                              ^^^^^^^^^^^^^^^^^^^^^^^^
                              这就是 spreadsheetToken
```

#### sheetId（工作表 ID）

电子表格中的单个工作表的唯一标识符。通过 `FsHelper.create()` 方法创建表格后会返回这个 ID。

#### titleRow（标题行）

表格中表头所在的行号（从 1 开始计数）。例如：
- `titleRow = 1` 表示表头在第 1 行
- `titleRow = 3` 表示表头在第 3 行

#### headLine（数据起始行 - 1）

表格中数据开始的行号（从 1 开始计数）。通常等于 `titleRow + 1`，但如果有描述行，可能需要加 2。

**示例**：
```
行1: [表头分组1] [表头分组2]  <- 表头分组行（可选）
行2: [列名1]     [列名2]      <- 标题行（titleRow = 2）
行3: [描述1]     [描述2]      <- 描述行（可选，enableDesc = true）
行4: [数据1]     [数据2]      <- 数据起始行（headLine = 3）
```

#### uniqueId（唯一标识）

系统根据唯一键字段自动计算的唯一标识符。用于：
- 数据去重
- Upsert 模式中的数据匹配
- 数据更新

#### Upsert 模式

Upsert（Update + Insert）模式是一种数据写入策略：
- **默认开启**（`upsert = true`）：根据唯一键匹配，如果数据已存在则更新，不存在则追加
- **关闭**（`upsert = false`）：不匹配唯一键，所有数据直接追加到表格末尾

---

## 4. 注解方式使用（实体类）

### 4.1 实体类定义

#### @TableConf 注解

`@TableConf` 注解用于配置整个表格的全局属性，必须放在实体类上。

**主要参数**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `uniKeys` | String[] | {} | 唯一键字段名数组，用于数据更新和去重 |
| `headLine` | int | 1 | 数据起始行号 |
| `titleRow` | int | 1 | 标题行行号 |
| `enableCover` | boolean | false | 是否覆盖已存在的数据 |
| `isText` | boolean | false | 是否设置表格为纯文本格式 |
| `enableDesc` | boolean | false | 是否启用字段描述行 |
| `headFontColor` | String | "#000000" | 表头字体颜色（十六进制） |
| `headBackColor` | String | "#cccccc" | 表头背景颜色（十六进制） |
| `upsert` | boolean | true | 是否启用 Upsert 模式 |

**示例**：

```java
@TableConf(
    headLine = 4,
    titleRow = 3,
    enableDesc = true,
    uniKeys = {"employeeId"},
    headFontColor = "#ffffff",
    headBackColor = "#1890ff"
)
public class Employee extends BaseEntity {
    // ...
}
```

#### @TableProperty 注解

`@TableProperty` 注解用于配置实体类字段与表格列的映射关系，必须放在字段上。

**主要参数**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | String[] | {} | 表格列名，支持多层级表头 |
| `desc` | String | "" | 字段描述 |
| `order` | int | Integer.MAX_VALUE | 字段排序顺序，数值越小越靠前 |
| `type` | TypeEnum | TEXT | 字段类型（文本、单选、多选等） |
| `enumClass` | Class | BaseEnum.class | 枚举类（用于单选/多选） |
| `fieldFormatClass` | Class | FieldValueProcess.class | 字段格式化处理类 |
| `optionsClass` | Class | OptionsValueProcess.class | 选项处理类 |

**示例**：

```java
// 单层级表头
@TableProperty(value = "姓名", order = 0)
private String name;

// 多层级表头
@TableProperty(value = {"ID", "员工信息", "员工编号"}, order = 0)
private String employeeId;

// 单选字段
@TableProperty(value = "状态", order = 1, type = TypeEnum.SINGLE_SELECT, enumClass = StatusEnum.class)
private String status;

// 带描述的字段
@TableProperty(value = "邮箱", order = 2, desc = "员工邮箱地址")
private String email;
```

#### BaseEntity 基类

所有使用注解方式的实体类都应该继承 `BaseEntity`，它提供了以下属性：

- `uniqueId`：唯一标识符（自动生成）
- `row`：数据所在的行号
- `rowData`：原始行数据（Map格式）

```java
public class Employee extends BaseEntity {
    // 你的字段定义
}
```

### 4.2 创建表格

#### 基础创建

使用 `FsHelper.create()` 方法创建表格：

```java
String sheetId = FsHelper.create("员工表", spreadsheetToken, Employee.class);
```

这个方法会：
1. 创建新的工作表
2. 根据实体类注解生成表头
3. 设置表格样式（字体、背景色等）
4. 合并多层级表头的单元格
5. 设置下拉选项（如果配置了枚举类）
6. 设置字段描述（如果启用了 `enableDesc`）

#### 使用 SheetBuilder 高级配置

`SheetBuilder` 提供了更灵活的配置方式，支持链式调用：

```java
String sheetId = FsHelper.createBuilder("员工表", spreadsheetToken, Employee.class)
    .includeFields("employeeId", "name", "email")  // 只包含指定字段
    .excludeFields("department")                    // 排除指定字段
    .customProperties(customProps)                  // 自定义属性
    .build();
```

**SheetBuilder 主要方法**：

- `includeFields(String... fields)`：只包含指定的字段
- `excludeFields(String... fields)`：排除指定的字段
- `customProperties(Map<String, Object> props)`：自定义属性，用于传递给选项处理类等

#### 多层级表头

多层级表头通过 `@TableProperty` 的 `value` 参数配置，使用字符串数组：

```java
@TableConf(headLine = 4, titleRow = 3)
public class Employee extends BaseEntity {

    // 三级表头：ID -> 员工信息 -> 员工编号
    @TableProperty(value = {"ID", "员工信息", "员工编号"}, order = 0)
    private String employeeId;

    // 三级表头：ID -> 员工信息 -> 姓名
    @TableProperty(value = {"ID", "员工信息", "姓名"}, order = 1)
    private String name;

    // 二级表头：联系方式 -> 邮箱
    @TableProperty(value = {"联系方式", "邮箱"}, order = 2)
    private String email;

    // 单级表头：部门
    @TableProperty(value = "部门", order = 3)
    private String department;
}
```

**表头结构示例**：

```
行1: [        ID         ]    [       ]      [    ]
行2: [      员工信息      ]    [联系方式]      [     ]
行3: [员工编号]   [ 姓名  ]     [ 邮箱  ]      [ 部门 ]
行4: [数据... ]   [数据...]     [数据...]     [数据...]
```

系统会自动合并相同值的单元格。

#### 分组表格

分组表格允许多个字段组，每个组包含相同的字段集合。例如，在问卷调查中，可能需要为多个受访者记录相同的信息。

**注意**：分组表格功能主要在 Map 配置方式中使用，注解方式通过多层级表头实现类似效果。

### 4.3 写入数据

#### 基础写入

使用 `FsHelper.write()` 方法写入数据：

```java
List<Employee> employees = new ArrayList<>();
// ... 准备数据
FsHelper.write(sheetId, spreadsheetToken, employees);
```

**写入逻辑**：
1. 根据 `uniKeys` 计算每条数据的 `uniqueId`
2. 在现有数据中查找匹配的 `uniqueId`
3. 如果找到（且 `upsert = true`），更新对应行
4. 如果没找到，追加到表格末尾

#### 使用 WriteBuilder 高级配置

```java
FsHelper.writeBuilder(sheetId, spreadsheetToken, employees)
    .ignoreUniqueFields("updateTime")  // 计算唯一ID时忽略指定字段
    .build();
```

**WriteBuilder 主要方法**：

- `ignoreUniqueFields(String... fields)`：计算唯一ID时忽略的字段

#### Upsert 模式

Upsert 模式是默认启用的，可以通过 `@TableConf(upsert = false)` 关闭。

**示例：启用 Upsert 模式**

```java
@TableConf(uniKeys = {"employeeId"}, upsert = true)
public class Employee extends BaseEntity {
    @TableProperty(value = "员工编号", order = 0)
    private String employeeId;
    // ...
}

// 写入数据
Employee emp1 = new Employee();
emp1.setEmployeeId("E001");
emp1.setName("张三");
employees.add(emp1);

FsHelper.write(sheetId, spreadsheetToken, employees);
// 如果表格中已存在 employeeId = "E001" 的记录，会更新该记录
// 如果不存在，会追加新记录
```

**示例：关闭 Upsert 模式（纯追加）**

```java
@TableConf(uniKeys = {"employeeId"}, upsert = false)
public class Employee extends BaseEntity {
    // ...
}

// 写入数据
FsHelper.write(sheetId, spreadsheetToken, employees);
// 所有数据都会追加到表格末尾，不会检查是否已存在
```

#### 图片上传

支持将图片上传到飞书表格单元格中：

```java
import cn.isliu.core.FileData;
import cn.isliu.core.enums.FileType;

public class Product extends BaseEntity {
    @TableProperty(value = "产品图片", order = 0)
    private FileData image;
    
    // getters and setters
}

// 写入数据
Product product = new Product();
FileData fileData = new FileData();
fileData.setFileName("product.jpg");
fileData.setImageData(imageBytes);  // 图片的字节数组
fileData.setFileType(FileType.IMAGE.getType());
product.setImage(fileData);

List<Product> products = new ArrayList<>();
products.add(product);
FsHelper.write(sheetId, spreadsheetToken, products);
```

### 4.4 读取数据

#### 基础读取

使用 `FsHelper.read()` 方法读取数据：

```java
List<Employee> employees = FsHelper.read(sheetId, spreadsheetToken, Employee.class);
```

**读取逻辑**：
1. 读取表格数据
2. 根据 `titleRow` 识别表头
3. 从 `headLine` 开始读取数据行
4. 将数据映射到实体类对象
5. 自动计算 `uniqueId` 并设置到 `BaseEntity`

#### 使用 ReadBuilder 高级配置

```java
List<Employee> employees = FsHelper.readBuilder(sheetId, spreadsheetToken, Employee.class)
    .ignoreUniqueFields("updateTime")  // 计算唯一ID时忽略指定字段
    .build();
```

**ReadBuilder 主要方法**：

- `ignoreUniqueFields(String... fields)`：计算唯一ID时忽略的字段

**读取后的对象属性**：

```java
for (Employee emp : employees) {
    // 业务字段
    System.out.println(emp.getName());
    
    // BaseEntity 提供的属性
    System.out.println("唯一ID: " + emp.getUniqueId());
    System.out.println("行号: " + emp.getRow());
    System.out.println("原始数据: " + emp.getRowData());
}
```

---

## 5. Map 配置方式使用

Map 配置方式适用于不需要定义实体类的场景，提供了更大的灵活性。

### 5.1 MapFieldDefinition 字段定义

`MapFieldDefinition` 用于定义单个字段的所有属性。

#### 快速创建方法

```java
import cn.isliu.core.config.MapFieldDefinition;

// 创建文本字段
MapFieldDefinition field1 = MapFieldDefinition.text("字段名", 0);

// 创建文本字段（带描述）
MapFieldDefinition field2 = MapFieldDefinition.text("字段名", 0, "字段描述");

// 创建单选字段
MapFieldDefinition field3 = MapFieldDefinition.singleSelect("状态", 1, "启用", "禁用");

// 创建多选字段
MapFieldDefinition field4 = MapFieldDefinition.multiSelect("标签", 2, "标签1", "标签2", "标签3");

// 使用枚举类创建单选字段
MapFieldDefinition field5 = MapFieldDefinition.singleSelectWithEnum("状态", 1, StatusEnum.class);
```

#### 使用 Builder 创建

```java
MapFieldDefinition field = MapFieldDefinition.builder()
    .fieldName("字段名")
    .order(0)
    .type(TypeEnum.TEXT)
    .description("字段描述")
    .required(true)
    .defaultValue("默认值")
    .build();
```

#### 字段类型

支持的类型（`TypeEnum`）：
- `TEXT`：文本
- `SINGLE_SELECT`：单选
- `MULTI_SELECT`：多选
- `NUMBER`：数字
- `DATE`：日期
- `TEXT_FILE`：文本文件
- `MULTI_TEXT`：多个文本（逗号分割）
- `TEXT_URL`：文本链接

### 5.2 MapSheetConfig 表格配置

`MapSheetConfig` 用于配置表格创建时的属性。

#### 基础配置

```java
import cn.isliu.core.config.MapSheetConfig;

MapSheetConfig config = MapSheetConfig.sheetBuilder()
    .titleRow(2)                    // 标题行
    .headLine(3)                    // 数据起始行
    .headStyle("#ffffff", "#000000") // 字体颜色，背景颜色
    .isText(false)                  // 是否纯文本格式
    .enableDesc(true)               // 是否启用描述
    .addField(MapFieldDefinition.text("字段1", 0))
    .addField(MapFieldDefinition.text("字段2", 1))
    .build();
```

#### 批量添加字段

```java
List<MapFieldDefinition> fields = new ArrayList<>();
fields.add(MapFieldDefinition.text("字段1", 0));
fields.add(MapFieldDefinition.text("字段2", 1));

MapSheetConfig config = MapSheetConfig.sheetBuilder()
    .addFields(fields)  // 批量添加
    .build();

// 或者使用可变参数
MapSheetConfig config2 = MapSheetConfig.sheetBuilder()
    .addFields(
        MapFieldDefinition.text("字段1", 0),
        MapFieldDefinition.text("字段2", 1)
    )
    .build();
```

### 5.3 MapTableConfig 读写配置

`MapTableConfig` 用于配置数据读写时的属性。

```java
import cn.isliu.core.config.MapTableConfig;

MapTableConfig config = MapTableConfig.builder()
    .titleRow(2)                    // 标题行
    .headLine(3)                    // 数据起始行
    .addUniKeyName("字段1")         // 添加唯一键
    .enableCover(false)             // 是否覆盖
    .upsert(true)                   // 是否启用 Upsert
    .ignoreNotFound(false)          // 是否忽略未找到的数据
    .build();
```

### 5.4 创建表格

#### 使用 createMapSheet

```java
MapSheetConfig config = MapSheetConfig.sheetBuilder()
    .titleRow(2)
    .headLine(3)
    .addField(MapFieldDefinition.text("姓名", 0))
    .addField(MapFieldDefinition.text("年龄", 1))
    .build();

String sheetId = FsHelper.createMapSheet("表格名称", spreadsheetToken, config);
```

#### 使用 MapSheetBuilder

```java
String sheetId = FsHelper.createMapSheetBuilder("表格名称", spreadsheetToken)
    .titleRow(2)
    .headLine(3)
    .headStyle("#ffffff", "#000000")
    .isText(false)
    .enableDesc(true)
    .addField(MapFieldDefinition.text("姓名", 0))
    .addField(MapFieldDefinition.text("年龄", 1))
    .build();
```

**MapSheetBuilder 主要方法**：

- `titleRow(int row)`：设置标题行
- `headLine(int line)`：设置数据起始行
- `headStyle(String fontColor, String backColor)`：设置表头样式
- `isText(boolean isText)`：是否纯文本格式
- `enableDesc(boolean enable)`：是否启用描述
- `addField(MapFieldDefinition field)`：添加单个字段
- `addFields(List<MapFieldDefinition> fields)`：批量添加字段
- `addFields(MapFieldDefinition... fields)`：批量添加字段（可变参数）
- `fields(List<MapFieldDefinition> fields)`：设置所有字段（覆盖现有）
- `groupFields(String... groupFields)`：设置分组字段（用于分组表格）

### 5.5 写入数据

#### 使用 writeMap

```java
List<Map<String, Object>> dataList = new ArrayList<>();
Map<String, Object> data = new HashMap<>();
data.put("姓名", "张三");
data.put("年龄", 25);
dataList.add(data);

MapTableConfig config = MapTableConfig.builder()
    .titleRow(2)
    .headLine(3)
    .addUniKeyName("姓名")
    .upsert(true)
    .build();

FsHelper.writeMap(sheetId, spreadsheetToken, dataList, config);
```

#### 使用 MapWriteBuilder

```java
List<Map<String, Object>> dataList = new ArrayList<>();
// ... 准备数据

FsHelper.writeMapBuilder(sheetId, spreadsheetToken, dataList)
    .titleRow(2)
    .headLine(3)
    .addUniKeyName("姓名")
    .upsert(true)
    .enableCover(false)
    .ignoreNotFound(false)
    .build();
```

**MapWriteBuilder 主要方法**：

- `titleRow(int row)`：设置标题行
- `headLine(int line)`：设置数据起始行
- `addUniKeyName(String name)`：添加唯一键字段名
- `upsert(boolean upsert)`：是否启用 Upsert
- `enableCover(boolean enable)`：是否覆盖已存在数据
- `ignoreNotFound(boolean ignore)`：是否忽略未找到的数据

### 5.6 读取数据

#### 使用 readMap

```java
MapTableConfig config = MapTableConfig.builder()
    .titleRow(2)
    .headLine(3)
    .addUniKeyName("姓名")
    .build();

List<Map<String, Object>> dataList = FsHelper.readMap(sheetId, spreadsheetToken, config);

for (Map<String, Object> data : dataList) {
    String name = (String) data.get("姓名");
    Integer age = (Integer) data.get("年龄");
    String uniqueId = (String) data.get("_uniqueId");  // 系统自动添加
    Integer rowNumber = (Integer) data.get("_rowNumber");  // 系统自动添加
}
```

**返回的 Map 中包含的特殊字段**：
- `_uniqueId`：根据唯一键计算的唯一标识
- `_rowNumber`：数据所在的行号（从 1 开始）

#### 使用 MapReadBuilder

```java
// 基础读取
List<Map<String, Object>> dataList = FsHelper.readMapBuilder(sheetId, spreadsheetToken)
    .titleRow(2)
    .headLine(3)
    .addUniKeyName("姓名")
    .build();

// 分组读取
Map<String, List<Map<String, Object>>> groupedData = FsHelper.readMapBuilder(sheetId, spreadsheetToken)
    .titleRow(2)
    .headLine(3)
    .groupBuild();  // 返回 Map<分组名, 数据列表>
```

**MapReadBuilder 主要方法**：

- `titleRow(int row)`：设置标题行
- `headLine(int line)`：设置数据起始行
- `addUniKeyName(String name)`：添加唯一键字段名
- `build()`：执行读取，返回 `List<Map<String, Object>>`
- `groupBuild()`：执行分组读取，返回 `Map<String, List<Map<String, Object>>>`

**注意**：
- 如果某行数据的所有业务字段值都为 `null` 或空字符串，该行数据将被自动过滤
- 分组读取时，如果某个分组下的某行数据所有字段值都为 `null` 或空字符串，该行数据也会被自动过滤

---

## 6. 高级特性

### 6.1 多层级表头

多层级表头用于创建复杂的表格结构，支持多级分组。

#### 配置方式

在注解方式中，通过 `@TableProperty` 的 `value` 参数使用字符串数组：

```java
@TableProperty(value = {"一级", "二级", "三级"}, order = 0)
private String field;
```

#### 合并规则

系统会自动合并相同值的单元格：

```java
@TableConf(headLine = 4, titleRow = 3)
public class Report extends BaseEntity {
    
    // 第一组：ID -> 员工信息 -> 员工编号
    @TableProperty(value = {"ID", "员工信息", "员工编号"}, order = 0)
    private String employeeId;
    
    // 第一组：ID -> 员工信息 -> 姓名
    @TableProperty(value = {"ID", "员工信息", "姓名"}, order = 1)
    private String name;
    
    // 第一组：ID -> 员工信息 -> 邮箱
    @TableProperty(value = {"ID", "员工信息", "邮箱"}, order = 2)
    private String email;
    
    // 第二组：联系方式 -> 电话
    @TableProperty(value = {"联系方式", "电话"}, order = 3)
    private String phone;
    
    // 第二组：联系方式 -> 地址
    @TableProperty(value = {"联系方式", "地址"}, order = 4)
    private String address;
}
```

**生成的表头结构**：

```
行1: [ID]                    [联系方式]
行2: [员工信息]              [联系方式]
行3: [员工编号] [姓名] [邮箱] [电话] [地址]
行4: [数据...]
```

#### 排序规则

多层级表头的排序遵循以下规则：
1. 相同第一层级的字段必须相邻
2. 在满足条件 1 的情况下，按 `order` 值排序
3. 三级及以上层级要求同一分组内的 `order` 必须连续

**示例**：

```java
// 正确：同一分组内 order 连续
@TableProperty(value = {"ID", "员工信息", "员工编号"}, order = 0)
@TableProperty(value = {"ID", "员工信息", "姓名"}, order = 1)
@TableProperty(value = {"ID", "员工信息", "邮箱"}, order = 2)

// 错误：同一分组内 order 不连续
@TableProperty(value = {"ID", "员工信息", "员工编号"}, order = 0)
@TableProperty(value = {"ID", "员工信息", "姓名"}, order = 5)  // 错误！
```

### 6.2 分组表格

分组表格允许为多个组创建相同的字段结构，适用于需要重复相同字段的场景。

#### Map 配置方式

```java
// 定义基础字段
List<MapFieldDefinition> baseFields = new ArrayList<>();
baseFields.add(MapFieldDefinition.text("产品名称", 0));
baseFields.add(MapFieldDefinition.text("价格", 1));
baseFields.add(MapFieldDefinition.text("库存", 2));

// 创建分组表格
String sheetId = FsHelper.createMapSheetBuilder("产品分组表", spreadsheetToken)
    .addFields(baseFields)
    .groupFields("分组A", "分组B", "分组C")  // 创建三个分组
    .build();
```

**生成的表格结构**：

```
行1: [分组A] [分组A] [分组A] [分组B] [分组B] [分组B] [分组C] [分组C] [分组C]
行2: [产品名称] [价格] [库存] [产品名称] [价格] [库存] [产品名称] [价格] [库存]
行3: [数据...]
```

#### 分组数据读取

```java
Map<String, List<Map<String, Object>>> groupedData = 
    FsHelper.readMapBuilder(sheetId, spreadsheetToken)
        .titleRow(2)
        .headLine(3)
        .groupBuild();

// 访问分组数据
List<Map<String, Object>> groupA = groupedData.get("分组A");
List<Map<String, Object>> groupB = groupedData.get("分组B");
```

### 6.3 字段类型与选项

#### 枚举类配置

使用枚举类可以自动生成下拉选项：

```java
// 定义枚举类
public enum StatusEnum implements BaseEnum {
    ENABLED("enabled", "启用"),
    DISABLED("disabled", "禁用");
    
    private final String code;
    private final String desc;
    
    StatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    @Override
    public String getCode() { return code; }
    
    @Override
    public String getDesc() { return desc; }
}

// 在实体类中使用
@TableProperty(value = "状态", order = 0, type = TypeEnum.SINGLE_SELECT, enumClass = StatusEnum.class)
private String status;
```

#### 动态选项配置

使用 `OptionsValueProcess` 接口可以实现动态选项：

```java
public class DynamicStatusOptions implements OptionsValueProcess {
    @Override
    public List<String> process(Object value) {
        // value 是一个 Map，包含 _field 等信息
        Map<String, Object> props = (Map<String, Object>) value;
        FieldProperty field = (FieldProperty) props.get("_field");
        
        // 根据业务逻辑返回选项列表
        List<String> options = new ArrayList<>();
        options.add("选项1");
        options.add("选项2");
        return options;
    }
}

// 在实体类中使用
@TableProperty(
    value = "状态", 
    order = 0, 
    type = TypeEnum.SINGLE_SELECT, 
    optionsClass = DynamicStatusOptions.class
)
private String status;
```

#### Map 配置方式

```java
// 直接指定选项
MapFieldDefinition field = MapFieldDefinition.singleSelect("状态", 0, "启用", "禁用");

// 使用枚举类
MapFieldDefinition field2 = MapFieldDefinition.singleSelectWithEnum("状态", 0, StatusEnum.class);

// 使用选项处理类
MapFieldDefinition field3 = MapFieldDefinition.builder()
    .fieldName("状态")
    .order(0)
    .type(TypeEnum.SINGLE_SELECT)
    .optionsClass(DynamicStatusOptions.class)
    .build();
```

### 6.4 字段格式化

使用 `FieldValueProcess` 接口可以自定义字段值的处理逻辑：

```java
public class DateFormatProcess implements FieldValueProcess<String> {
    @Override
    public String process(Object value) {
        // 将 Date 对象格式化为字符串
        if (value instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.format((Date) value);
        }
        return value != null ? value.toString() : null;
    }
    
    @Override
    public Object reverseProcess(Object value) {
        // 将字符串解析为 Date 对象
        if (value instanceof String) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                return sdf.parse((String) value);
            } catch (Exception e) {
                return null;
            }
        }
        return value;
    }
}

// 在实体类中使用
@TableProperty(
    value = "创建日期", 
    order = 0,
    fieldFormatClass = DateFormatProcess.class
)
private Date createDate;
```

**注意**：
- `process()` 方法用于写入时的格式化（对象 -> 表格值）
- `reverseProcess()` 方法用于读取时的解析（表格值 -> 对象）

### 6.5 唯一键与 Upsert

#### 唯一键配置

唯一键用于标识表格中的唯一记录，通过 `@TableConf(uniKeys = {...})` 或 `MapTableConfig.addUniKeyName()` 配置。

**示例：单字段唯一键**

```java
@TableConf(uniKeys = {"employeeId"})
public class Employee extends BaseEntity {
    @TableProperty(value = "员工编号", order = 0)
    private String employeeId;
    // ...
}
```

**示例：多字段唯一键**

```java
@TableConf(uniKeys = {"employeeId", "department"})
public class Employee extends BaseEntity {
    @TableProperty(value = "员工编号", order = 0)
    private String employeeId;
    
    @TableProperty(value = "部门", order = 1)
    private String department;
    // ...
}
```

#### Upsert 工作原理

1. **计算唯一ID**：根据唯一键字段的值计算 SHA256 哈希值
2. **查找匹配**：在现有数据中查找相同 `uniqueId` 的记录
3. **更新或追加**：
   - 如果找到且 `upsert = true`：更新该行的所有字段
   - 如果没找到或 `upsert = false`：追加到表格末尾

**示例**：

```java
// 第一次写入
Employee emp1 = new Employee();
emp1.setEmployeeId("E001");
emp1.setName("张三");
// 写入表格，追加到末尾

// 第二次写入（更新）
Employee emp2 = new Employee();
emp2.setEmployeeId("E001");  // 相同的唯一键
emp2.setName("张三（已更新）");
// 由于 employeeId 相同，会更新之前的记录，而不是追加新记录
```

#### 纯追加模式

设置 `upsert = false` 可以关闭 Upsert 模式，所有数据都会追加到表格末尾：

```java
@TableConf(uniKeys = {"employeeId"}, upsert = false)
public class Employee extends BaseEntity {
    // ...
}

// 即使 employeeId 相同，也会追加新记录
FsHelper.write(sheetId, spreadsheetToken, employees);
```

### 6.6 图片上传

支持将图片上传到飞书表格单元格中。

#### 实体类方式

```java
import cn.isliu.core.FileData;
import cn.isliu.core.enums.FileType;

public class Product extends BaseEntity {
    @TableProperty(value = "产品图片", order = 0)
    private FileData image;
    
    // getters and setters
}

// 写入数据
Product product = new Product();
FileData fileData = new FileData();
fileData.setFileName("product.jpg");
fileData.setImageData(imageBytes);  // 图片的字节数组
fileData.setFileType(FileType.IMAGE.getType());
product.setImage(fileData);

List<Product> products = new ArrayList<>();
products.add(product);
FsHelper.write(sheetId, spreadsheetToken, products);
```

#### Map 配置方式

```java
Map<String, Object> data = new HashMap<>();
FileData fileData = new FileData();
fileData.setFileName("product.jpg");
fileData.setImageData(imageBytes);
fileData.setFileType(FileType.IMAGE.getType());
data.put("产品图片", fileData);

List<Map<String, Object>> dataList = new ArrayList<>();
dataList.add(data);

FsHelper.writeMap(sheetId, spreadsheetToken, dataList, config);
```

**注意事项**：
- 图片数据必须是字节数组（`byte[]`）
- 支持常见的图片格式（JPG、PNG等）
- 图片上传是异步进行的，写入方法返回后可能还在上传中

---

## 7. 实际应用场景

### 7.1 场景一：员工信息管理

#### 需求描述

需要将员工信息同步到飞书表格，支持员工信息的增删改查。

#### 实体类设计

```java
import cn.isliu.core.BaseEntity;
import cn.isliu.core.annotation.TableConf;
import cn.isliu.core.annotation.TableProperty;

@TableConf(
    headLine = 2,
    titleRow = 1,
    enableDesc = true,
    uniKeys = {"employeeId"},
    headFontColor = "#ffffff",
    headBackColor = "#1890ff"
)
public class Employee extends BaseEntity {

    @TableProperty(value = "员工编号", order = 0, desc = "员工编号，唯一标识")
    private String employeeId;

    @TableProperty(value = "姓名", order = 1, desc = "员工姓名")
    private String name;

    @TableProperty(value = "部门", order = 2, desc = "所属部门")
    private String department;

    @TableProperty(value = "职位", order = 3, desc = "职位名称")
    private String position;

    @TableProperty(value = "邮箱", order = 4, desc = "工作邮箱")
    private String email;

    @TableProperty(value = "电话", order = 5, desc = "联系电话")
    private String phone;

    // getters and setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
```

#### 完整代码示例

```java
import cn.isliu.FsHelper;
import cn.isliu.core.client.FsClient;
import java.util.ArrayList;
import java.util.List;

public class EmployeeManagement {
    
    private String spreadsheetToken = "your_spreadsheet_token";
    private String sheetId;
    
    public void init() {
        try (FsClient fsClient = FsClient.getInstance()) {
            fsClient.initializeClient("your_app_id", "your_app_secret");
            
            // 创建员工表
            sheetId = FsHelper.create("员工信息表", spreadsheetToken, Employee.class);
            System.out.println("员工表创建成功，Sheet ID: " + sheetId);
        }
    }
    
    public void addEmployee(String employeeId, String name, String department, 
                           String position, String email, String phone) {
        try (FsClient fsClient = FsClient.getInstance()) {
            fsClient.initializeClient("your_app_id", "your_app_secret");
            
            Employee emp = new Employee();
            emp.setEmployeeId(employeeId);
            emp.setName(name);
            emp.setDepartment(department);
            emp.setPosition(position);
            emp.setEmail(email);
            emp.setPhone(phone);
            
            List<Employee> employees = new ArrayList<>();
            employees.add(emp);
            
            FsHelper.write(sheetId, spreadsheetToken, employees);
            System.out.println("员工添加成功: " + name);
        }
    }
    
    public void updateEmployee(String employeeId, String name, String department) {
        try (FsClient fsClient = FsClient.getInstance()) {
            fsClient.initializeClient("your_app_id", "your_app_secret");
            
            Employee emp = new Employee();
            emp.setEmployeeId(employeeId);  // 唯一键，用于匹配
            emp.setName(name);
            emp.setDepartment(department);
            
            List<Employee> employees = new ArrayList<>();
            employees.add(emp);
            
            // Upsert 模式会自动更新已存在的记录
            FsHelper.write(sheetId, spreadsheetToken, employees);
            System.out.println("员工更新成功: " + name);
        }
    }
    
    public List<Employee> getAllEmployees() {
        try (FsClient fsClient = FsClient.getInstance()) {
            fsClient.initializeClient("your_app_id", "your_app_secret");
            
            List<Employee> employees = FsHelper.read(sheetId, spreadsheetToken, Employee.class);
            return employees;
        }
    }
    
    public Employee getEmployeeById(String employeeId) {
        List<Employee> employees = getAllEmployees();
        return employees.stream()
            .filter(emp -> employeeId.equals(emp.getEmployeeId()))
            .findFirst()
            .orElse(null);
    }
}
```

### 7.2 场景二：动态表单数据采集

#### 需求描述

需要根据配置动态创建表单，收集用户数据，并回读到系统中。

#### Map 配置方式

```java
import cn.isliu.FsHelper;
import cn.isliu.core.config.MapFieldDefinition;
import cn.isliu.core.config.MapSheetConfig;
import cn.isliu.core.config.MapTableConfig;
import java.util.*;

public class DynamicFormCollector {
    
    private String spreadsheetToken = "your_spreadsheet_token";
    
    /**
     * 根据配置创建动态表单
     */
    public String createForm(String formName, List<FormFieldConfig> fieldConfigs) {
        try (FsClient fsClient = FsClient.getInstance()) {
            fsClient.initializeClient("your_app_id", "your_app_secret");
            
            MapSheetConfig config = MapSheetConfig.sheetBuilder()
                .titleRow(1)
                .headLine(2)
                .enableDesc(true);
            
            // 根据配置动态添加字段
            for (int i = 0; i < fieldConfigs.size(); i++) {
                FormFieldConfig fieldConfig = fieldConfigs.get(i);
                MapFieldDefinition field = createField(fieldConfig, i);
                config.addField(field);
            }
            
            String sheetId = FsHelper.createMapSheet(formName, spreadsheetToken, config);
            return sheetId;
        }
    }
    
    /**
     * 根据字段配置创建字段定义
     */
    private MapFieldDefinition createField(FormFieldConfig config, int order) {
        MapFieldDefinition.Builder builder = MapFieldDefinition.builder()
            .fieldName(config.getFieldName())
            .order(order)
            .description(config.getDescription());
        
        switch (config.getType()) {
            case "text":
                builder.type(TypeEnum.TEXT);
                break;
            case "single_select":
                builder.type(TypeEnum.SINGLE_SELECT)
                       .options(config.getOptions());
                break;
            case "multi_select":
                builder.type(TypeEnum.MULTI_SELECT)
                       .options(config.getOptions());
                break;
            case "number":
                builder.type(TypeEnum.NUMBER);
                break;
            case "date":
                builder.type(TypeEnum.DATE);
                break;
            default:
                builder.type(TypeEnum.TEXT);
        }
        
        return builder.build();
    }
    
    /**
     * 读取表单数据
     */
    public List<Map<String, Object>> readFormData(String sheetId) {
        try (FsClient fsClient = FsClient.getInstance()) {
            fsClient.initializeClient("your_app_id", "your_app_secret");
            
            MapTableConfig config = MapTableConfig.builder()
                .titleRow(1)
                .headLine(2)
                .build();
            
            return FsHelper.readMap(sheetId, spreadsheetToken, config);
        }
    }
    
    /**
     * 字段配置类
     */
    public static class FormFieldConfig {
        private String fieldName;
        private String type;
        private String description;
        private List<String> options;
        
        // getters and setters
        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
    }
}
```

#### 使用示例

```java
// 创建表单配置
List<FormFieldConfig> fieldConfigs = new ArrayList<>();

FormFieldConfig nameField = new FormFieldConfig();
nameField.setFieldName("姓名");
nameField.setType("text");
nameField.setDescription("请输入您的姓名");
fieldConfigs.add(nameField);

FormFieldConfig genderField = new FormFieldConfig();
genderField.setFieldName("性别");
genderField.setType("single_select");
genderField.setOptions(Arrays.asList("男", "女"));
genderField.setDescription("请选择性别");
fieldConfigs.add(genderField);

FormFieldConfig hobbyField = new FormFieldConfig();
hobbyField.setFieldName("爱好");
hobbyField.setType("multi_select");
hobbyField.setOptions(Arrays.asList("阅读", "运动", "音乐", "旅行"));
hobbyField.setDescription("请选择您的爱好（可多选）");
fieldConfigs.add(hobbyField);

// 创建表单
DynamicFormCollector collector = new DynamicFormCollector();
String sheetId = collector.createForm("用户调查表", fieldConfigs);

// 读取表单数据
List<Map<String, Object>> formData = collector.readFormData(sheetId);
for (Map<String, Object> data : formData) {
    System.out.println("姓名: " + data.get("姓名"));
    System.out.println("性别: " + data.get("性别"));
    System.out.println("爱好: " + data.get("爱好"));
}
```

### 7.3 场景三：数据同步与更新

#### 需求描述

需要定期将业务系统的数据同步到飞书表格，支持增量更新。

#### 完整代码示例

```java
import cn.isliu.FsHelper;
import cn.isliu.core.client.FsClient;
import java.util.ArrayList;
import java.util.List;

@TableConf(
    headLine = 2,
    titleRow = 1,
    uniKeys = {"orderId"},
    upsert = true  // 启用 Upsert 模式
)
public class Order extends BaseEntity {
    
    @TableProperty(value = "订单编号", order = 0)
    private String orderId;
    
    @TableProperty(value = "客户名称", order = 1)
    private String customerName;
    
    @TableProperty(value = "订单金额", order = 2)
    private Double amount;
    
    @TableProperty(value = "订单状态", order = 3)
    private String status;
    
    @TableProperty(value = "创建时间", order = 4)
    private String createTime;
    
    // getters and setters
    // ...
}

public class OrderSyncService {
    
    private String spreadsheetToken = "your_spreadsheet_token";
    private String sheetId;
    
    public void init() {
        try (FsClient fsClient = FsClient.getInstance()) {
            fsClient.initializeClient("your_app_id", "your_app_secret");
            sheetId = FsHelper.create("订单表", spreadsheetToken, Order.class);
        }
    }
    
    /**
     * 同步订单数据（增量更新）
     */
    public void syncOrders(List<Order> orders) {
        try (FsClient fsClient = FsClient.getInstance()) {
            fsClient.initializeClient("your_app_id", "your_app_secret");
            
            // Upsert 模式会自动处理：已存在的订单会更新，新订单会追加
            FsHelper.write(sheetId, spreadsheetToken, orders);
            
            System.out.println("同步完成，处理了 " + orders.size() + " 条订单");
        }
    }
    
    /**
     * 从业务系统获取订单数据
     */
    private List<Order> fetchOrdersFromBusinessSystem() {
        // 模拟从业务系统获取数据
        List<Order> orders = new ArrayList<>();
        // ... 获取逻辑
        return orders;
    }
    
    /**
     * 定时同步任务
     */
    public void scheduleSync() {
        // 可以使用定时任务框架（如 Quartz、Spring Task 等）
        // 这里只是示例
        List<Order> orders = fetchOrdersFromBusinessSystem();
        syncOrders(orders);
    }
}
```

**关键点**：
- 使用 `uniKeys = {"orderId"}` 确保订单唯一性
- 使用 `upsert = true` 实现增量更新
- 已存在的订单会自动更新，新订单会自动追加

### 7.4 场景四：多层级报表生成

#### 需求描述

需要生成包含多层级表头的复杂报表，例如销售报表。

#### 完整代码示例

```java
import cn.isliu.FsHelper;
import cn.isliu.core.BaseEntity;
import cn.isliu.core.annotation.TableConf;
import cn.isliu.core.annotation.TableProperty;
import cn.isliu.core.client.FsClient;

@TableConf(
    headLine = 4,
    titleRow = 3,
    enableDesc = true,
    headFontColor = "#ffffff",
    headBackColor = "#1e88e5"
)
public class SalesReport extends BaseEntity {
    
    // 第一层级：基本信息
    @TableProperty(value = {"基本信息", "销售日期"}, order = 0, desc = "销售发生的日期")
    private String salesDate;
    
    @TableProperty(value = {"基本信息", "销售人员"}, order = 1, desc = "负责销售的员工姓名")
    private String salesPerson;
    
    // 第二层级：产品信息
    @TableProperty(value = {"产品信息", "产品名称"}, order = 2, desc = "销售的产品名称")
    private String productName;
    
    @TableProperty(value = {"产品信息", "产品类别"}, order = 3, desc = "产品所属类别")
    private String productCategory;
    
    // 第三层级：销售数据
    @TableProperty(value = {"销售数据", "销售数量"}, order = 4, desc = "销售的产品数量")
    private Integer quantity;
    
    @TableProperty(value = {"销售数据", "单价"}, order = 5, desc = "产品销售单价（元）")
    private Double unitPrice;
    
    @TableProperty(value = {"销售数据", "总金额"}, order = 6, desc = "销售总金额（元）")
    private Double totalAmount;
    
    // getters and setters
    // ...
}

public class SalesReportService {
    
    private String spreadsheetToken = "your_spreadsheet_token";
    private String sheetId;
    
    public void createReport() {
        try (FsClient fsClient = FsClient.getInstance()) {
            fsClient.initializeClient("your_app_id", "your_app_secret");
            sheetId = FsHelper.create("销售报表", spreadsheetToken, SalesReport.class);
            System.out.println("报表创建成功，Sheet ID: " + sheetId);
        }
    }
    
    public void updateReport(List<SalesReport> reports) {
        try (FsClient fsClient = FsClient.getInstance()) {
            fsClient.initializeClient("your_app_id", "your_app_secret");
            FsHelper.write(sheetId, spreadsheetToken, reports);
        }
    }
}
```

**关键点**：
- 使用数组形式的 `value` 定义多层级表头：`{"基本信息", "销售日期"}`
- 第一层级是 "基本信息"，第二层级是 "销售日期"
- 相同第一层级的字段会自动合并表头
- `headLine = 3` 表示数据从第4行开始（前3行是表头）

### 7.5 场景五：分组数据统计

#### 需求描述

需要创建分组表格，按不同维度统计和展示数据，例如按部门分组显示员工信息。

#### 完整代码示例

```java
import cn.isliu.FsHelper;
import cn.isliu.core.BaseEntity;
import cn.isliu.core.annotation.TableConf;
import cn.isliu.core.annotation.TableProperty;
import cn.isliu.core.client.FsClient;
import cn.isliu.core.config.MapFieldDefinition;
import cn.isliu.core.config.MapSheetConfig;
import java.util.*;

@TableConf(
    headLine = 3,
    titleRow = 2
)
public class Employee extends BaseEntity {
    
    @TableProperty(value = "员工编号", order = 0)
    private String employeeId;
    
    @TableProperty(value = "姓名", order = 1)
    private String name;
    
    @TableProperty(value = "邮箱", order = 2)
    private String email;
    
    @TableProperty(value = "部门", order = 3)
    private String department;
    
    // getters and setters
    // ...
}

public class GroupedEmployeeService {
    
    private String spreadsheetToken = "your_spreadsheet_token";
    
    /**
     * 创建分组表格（使用 Map 方式）
     */
    public String createGroupedSheet() {
        try (FsClient fsClient = FsClient.getInstance()) {
            fsClient.initializeClient("your_app_id", "your_app_secret");
            
            // 定义基础字段
            List<MapFieldDefinition> baseFields = Arrays.asList(
                MapFieldDefinition.text("员工编号", 0),
                MapFieldDefinition.text("姓名", 1),
                MapFieldDefinition.text("邮箱", 2)
            );
            
            // 创建分组表格配置
            MapSheetConfig config = MapSheetConfig.sheetBuilder()
                .titleRow(2)
                .headLine(3)
                .addFields(baseFields)
                .addGroupField("技术部")  // 第一组
                .addGroupField("销售部")  // 第二组
                .addGroupField("人事部")  // 第三组
                .build();
            
            return FsHelper.createMapSheet("分组员工表", spreadsheetToken, config);
        }
    }
    
    /**
     * 读取分组数据
     */
    public Map<String, List<Map<String, Object>>> readGroupedData(String sheetId) {
        try (FsClient fsClient = FsClient.getInstance()) {
            fsClient.initializeClient("your_app_id", "your_app_secret");
            
            MapTableConfig config = MapTableConfig.builder()
                .titleRow(2)
                .headLine(3)
                .build();
            
            // 使用 groupBuild() 方法获取分组数据
            return FsHelper.readMapBuilder(sheetId, spreadsheetToken)
                .config(config)
                .groupBuild();
        }
    }
    
    /**
     * 使用示例
     */
    public void example() {
        // 创建分组表格
        String sheetId = createGroupedSheet();
        
        // 读取分组数据
        Map<String, List<Map<String, Object>>> groupedData = readGroupedData(sheetId);
        
        // 处理每个分组的数据
        for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
            String groupName = entry.getKey();  // 如 "技术部"
            List<Map<String, Object>> employees = entry.getValue();
            
            System.out.println("=== " + groupName + " ===");
            for (Map<String, Object> employee : employees) {
                System.out.println("员工编号: " + employee.get("员工编号"));
                System.out.println("姓名: " + employee.get("姓名"));
                System.out.println("邮箱: " + employee.get("邮箱"));
            }
        }
    }
}
```

**关键点**：
- 使用 `addGroupField()` 方法添加分组字段
- 每个分组会重复显示基础字段列表
- 使用 `groupBuild()` 方法读取分组数据，返回 `Map<String, List<Map<String, Object>>>`
- Map 的 key 是分组名称，value 是该分组下的数据列表

---

## 8. API 参考

### 8.1 FsHelper 核心方法

#### 8.1.1 创建表格

##### `create(String sheetName, String spreadsheetToken, Class<T> clazz)`

根据实体类创建飞书表格。

**参数**：
- `sheetName`: 工作表名称
- `spreadsheetToken`: 电子表格 Token
- `clazz`: 实体类 Class 对象

**返回**：创建成功返回工作表 ID（String）

**示例**：
```java
String sheetId = FsHelper.create("员工表", spreadsheetToken, Employee.class);
```

##### `createBuilder(String sheetName, String spreadsheetToken, Class<T> clazz)`

创建表格构建器，支持高级配置。

**返回**：`SheetBuilder<T>` 实例

**示例**：
```java
String sheetId = FsHelper.createBuilder("员工表", spreadsheetToken, Employee.class)
    .includeFields("name", "email")  // 只包含指定字段
    .build();
```

##### `createMapSheet(String sheetName, String spreadsheetToken, MapSheetConfig config)`

使用 Map 配置创建表格。

**参数**：
- `sheetName`: 工作表名称
- `spreadsheetToken`: 电子表格 Token
- `config`: 表格配置对象

**返回**：创建成功返回工作表 ID（String）

**示例**：
```java
MapSheetConfig config = MapSheetConfig.sheetBuilder()
    .titleRow(2)
    .headLine(3)
    .addField(MapFieldDefinition.text("姓名", 0))
    .build();
String sheetId = FsHelper.createMapSheet("表格", spreadsheetToken, config);
```

##### `createMapSheetBuilder(String sheetName, String spreadsheetToken)`

创建 Map 表格构建器。

**返回**：`MapSheetBuilder` 实例

**示例**：
```java
String sheetId = FsHelper.createMapSheetBuilder("表格", spreadsheetToken)
    .titleRow(2)
    .headLine(3)
    .addField(MapFieldDefinition.text("姓名", 0))
    .build();
```

#### 8.1.2 写入数据

##### `write(String sheetId, String spreadsheetToken, List<T> dataList)`

将实体类对象列表写入表格。

**参数**：
- `sheetId`: 工作表 ID
- `spreadsheetToken`: 电子表格 Token
- `dataList`: 实体类对象列表

**返回**：写入操作结果（Object）

**示例**：
```java
List<Employee> employees = new ArrayList<>();
// ... 填充数据
FsHelper.write(sheetId, spreadsheetToken, employees);
```

##### `writeBuilder(String sheetId, String spreadsheetToken, List<T> dataList)`

创建写入构建器，支持高级配置。

**返回**：`WriteBuilder<T>` 实例

**示例**：
```java
FsHelper.writeBuilder(sheetId, spreadsheetToken, employees)
    .ignoreUniqueFields("createTime")  // 忽略指定字段
    .build();
```

##### `writeMap(String sheetId, String spreadsheetToken, List<Map<String, Object>> dataList, MapTableConfig config)`

使用 Map 数据写入表格。

**参数**：
- `sheetId`: 工作表 ID
- `spreadsheetToken`: 电子表格 Token
- `dataList`: Map 数据列表
- `config`: 表格配置对象

**返回**：写入操作结果（Object）

**示例**：
```java
List<Map<String, Object>> dataList = new ArrayList<>();
Map<String, Object> data = new HashMap<>();
data.put("姓名", "张三");
data.put("邮箱", "zhangsan@example.com");
dataList.add(data);

MapTableConfig config = MapTableConfig.builder()
    .titleRow(1)
    .headLine(2)
    .addUniKeyName("姓名")
    .build();

FsHelper.writeMap(sheetId, spreadsheetToken, dataList, config);
```

##### `writeMapBuilder(String sheetId, String spreadsheetToken, List<Map<String, Object>> dataList)`

创建 Map 写入构建器。

**返回**：`MapWriteBuilder` 实例

**示例**：
```java
FsHelper.writeMapBuilder(sheetId, spreadsheetToken, dataList)
    .titleRow(1)
    .headLine(2)
    .addUniKeyName("姓名")
    .enableCover(true)
    .build();
```

#### 8.1.3 读取数据

##### `read(String sheetId, String spreadsheetToken, Class<T> clazz)`

从表格读取数据并映射到实体类。

**参数**：
- `sheetId`: 工作表 ID
- `spreadsheetToken`: 电子表格 Token
- `clazz`: 实体类 Class 对象

**返回**：实体类对象列表 `List<T>`

**示例**：
```java
List<Employee> employees = FsHelper.read(sheetId, spreadsheetToken, Employee.class);
```

##### `readBuilder(String sheetId, String spreadsheetToken, Class<T> clazz)`

创建读取构建器，支持高级配置。

**返回**：`ReadBuilder<T>` 实例

**示例**：
```java
List<Employee> employees = FsHelper.readBuilder(sheetId, spreadsheetToken, Employee.class)
    .ignoreUniqueFields("updateTime")
    .build();
```

##### `readMap(String sheetId, String spreadsheetToken, MapTableConfig config)`

从表格读取数据并转换为 Map 格式。

**参数**：
- `sheetId`: 工作表 ID
- `spreadsheetToken`: 电子表格 Token
- `config`: 表格配置对象

**返回**：Map 数据列表 `List<Map<String, Object>>`

**注意**：返回的 Map 中包含两个特殊字段：
- `_uniqueId`: 唯一标识
- `_rowNumber`: 行号（从1开始）

**示例**：
```java
MapTableConfig config = MapTableConfig.builder()
    .titleRow(1)
    .headLine(2)
    .addUniKeyName("姓名")
    .build();

List<Map<String, Object>> dataList = FsHelper.readMap(sheetId, spreadsheetToken, config);
```

##### `readMapBuilder(String sheetId, String spreadsheetToken)`

创建 Map 读取构建器。

**返回**：`MapReadBuilder` 实例

**示例**：
```java
// 普通读取
List<Map<String, Object>> dataList = FsHelper.readMapBuilder(sheetId, spreadsheetToken)
    .titleRow(1)
    .headLine(2)
    .addUniKeyName("姓名")
    .build();

// 分组读取
Map<String, List<Map<String, Object>>> groupedData = FsHelper.readMapBuilder(sheetId, spreadsheetToken)
    .titleRow(2)
    .headLine(3)
    .groupBuild();
```

### 8.2 注解详解

#### 8.2.1 @TableConf

用于配置表格的全局属性。

**参数说明**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `uniKeys` | String[] | `{}` | 唯一键字段名数组，用于 Upsert 操作 |
| `headLine` | int | `1` | 数据起始行行号（从1开始） |
| `titleRow` | int | `1` | 标题行行号（从1开始） |
| `enableCover` | boolean | `false` | 是否覆盖已存在数据 |
| `isText` | boolean | `false` | 是否设置表格为纯文本格式 |
| `enableDesc` | boolean | `false` | 是否启用字段描述行 |
| `headFontColor` | String | `"#000000"` | 表头字体颜色（十六进制） |
| `headBackColor` | String | `"#cccccc"` | 表头背景颜色（十六进制） |
| `upsert` | boolean | `true` | 是否启用 Upsert 模式 |

**示例**：
```java
@TableConf(
    headLine = 4,
    titleRow = 3,
    uniKeys = {"employeeId"},
    enableDesc = true,
    upsert = true
)
public class Employee extends BaseEntity {
    // ...
}
```

#### 8.2.2 @TableProperty

用于标记实体类字段与表格列的映射关系。

**参数说明**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | String[] | `{}` | 表格列名，支持多层级（如 `{"基本信息", "姓名"}`） |
| `desc` | String | `""` | 字段描述 |
| `field` | String | `""` | 字段名（一般不需要手动指定） |
| `order` | int | `Integer.MAX_VALUE` | 字段排序顺序，数值越小越靠前 |
| `type` | TypeEnum | `TypeEnum.TEXT` | 字段类型 |
| `enumClass` | Class | `BaseEnum.class` | 枚举类（用于单选/多选） |
| `fieldFormatClass` | Class | `FieldValueProcess.class` | 字段格式化处理类 |
| `optionsClass` | Class | `OptionsValueProcess.class` | 选项处理类 |

**示例**：
```java
@TableProperty(
    value = {"基本信息", "姓名"},
    order = 0,
    desc = "员工姓名，不超过20个字符",
    type = TypeEnum.TEXT
)
private String name;
```

### 8.3 配置类详解

#### 8.3.1 MapSheetConfig

用于创建表格的配置类。

**主要方法**：

| 方法 | 说明 | 返回类型 |
|------|------|----------|
| `titleRow(int)` | 设置标题行行号 | `MapSheetConfig` |
| `headLine(int)` | 设置数据起始行行号 | `MapSheetConfig` |
| `headStyle(String fontColor, String backColor)` | 设置表头样式 | `MapSheetConfig` |
| `isText(boolean)` | 设置是否为纯文本格式 | `MapSheetConfig` |
| `enableDesc(boolean)` | 启用字段描述行 | `MapSheetConfig` |
| `addField(MapFieldDefinition)` | 添加单个字段 | `MapSheetConfig` |
| `addFields(List<MapFieldDefinition>)` | 批量添加字段 | `MapSheetConfig` |
| `addGroupField(String)` | 添加分组字段 | `MapSheetConfig` |

**示例**：
```java
MapSheetConfig config = MapSheetConfig.sheetBuilder()
    .titleRow(2)
    .headLine(3)
    .headStyle("#ffffff", "#1e88e5")
    .isText(true)
    .enableDesc(true)
    .addField(MapFieldDefinition.text("姓名", 0))
    .addField(MapFieldDefinition.text("邮箱", 1))
    .build();
```

#### 8.3.2 MapTableConfig

用于读写操作的配置类。

**主要方法**：

| 方法 | 说明 | 返回类型 |
|------|------|----------|
| `titleRow(int)` | 设置标题行行号 | `MapTableConfig` |
| `headLine(int)` | 设置数据起始行行号 | `MapTableConfig` |
| `addUniKeyName(String)` | 添加唯一键字段名 | `MapTableConfig` |
| `enableCover(boolean)` | 设置是否覆盖已存在数据 | `MapTableConfig` |
| `ignoreNotFound(boolean)` | 设置是否忽略未找到的数据 | `MapTableConfig` |
| `upsert(boolean)` | 设置是否启用 Upsert 模式 | `MapTableConfig` |

**示例**：
```java
MapTableConfig config = MapTableConfig.builder()
    .titleRow(1)
    .headLine(2)
    .addUniKeyName("employeeId")
    .enableCover(true)
    .upsert(true)
    .build();
```

#### 8.3.3 MapFieldDefinition

字段定义类，用于定义表格字段的属性。

**静态工厂方法**：

| 方法 | 说明 |
|------|------|
| `text(String fieldName, int order)` | 创建文本字段 |
| `text(String fieldName, int order, String description)` | 创建带描述的文本字段 |
| `singleSelect(String fieldName, int order, String... options)` | 创建单选字段 |
| `multiSelect(String fieldName, int order, String... options)` | 创建多选字段 |
| `singleSelectWithEnum(String fieldName, int order, Class<? extends BaseEnum> enumClass)` | 使用枚举创建单选字段 |
| `multiSelectWithEnum(String fieldName, int order, Class<? extends BaseEnum> enumClass)` | 使用枚举创建多选字段 |

**Builder 方法**：

| 方法 | 说明 | 返回类型 |
|------|------|----------|
| `fieldName(String)` | 设置字段名 | `Builder` |
| `description(String)` | 设置字段描述 | `Builder` |
| `order(int)` | 设置排序顺序 | `Builder` |
| `type(TypeEnum)` | 设置字段类型 | `Builder` |
| `options(List<String>)` | 设置选项列表 | `Builder` |
| `enumClass(Class)` | 设置枚举类 | `Builder` |
| `required(boolean)` | 设置是否必填 | `Builder` |
| `defaultValue(String)` | 设置默认值 | `Builder` |

**示例**：
```java
// 使用静态方法
MapFieldDefinition nameField = MapFieldDefinition.text("姓名", 0, "员工姓名");

// 使用 Builder
MapFieldDefinition emailField = MapFieldDefinition.builder()
    .fieldName("邮箱")
    .order(1)
    .type(TypeEnum.TEXT)
    .description("员工邮箱地址")
    .required(true)
    .build();

// 单选字段
MapFieldDefinition statusField = MapFieldDefinition.singleSelect(
    "状态", 2, "启用", "禁用"
);
```

### 8.4 Builder 类详解

#### 8.4.1 SheetBuilder

实体类表格创建构建器。

**主要方法**：

| 方法 | 说明 | 返回类型 |
|------|------|----------|
| `includeFields(String... fields)` | 只包含指定字段 | `SheetBuilder<T>` |
| `excludeFields(String... fields)` | 排除指定字段 | `SheetBuilder<T>` |
| `build()` | 构建并创建表格 | `String` |

**示例**：
```java
String sheetId = FsHelper.createBuilder("员工表", spreadsheetToken, Employee.class)
    .includeFields("name", "email", "department")
    .build();
```

#### 8.4.2 WriteBuilder

实体类数据写入构建器。

**主要方法**：

| 方法 | 说明 | 返回类型 |
|------|------|----------|
| `ignoreUniqueFields(String... fields)` | 忽略指定唯一字段 | `WriteBuilder<T>` |
| `build()` | 构建并执行写入 | `Object` |

**示例**：
```java
FsHelper.writeBuilder(sheetId, spreadsheetToken, employees)
    .ignoreUniqueFields("createTime", "updateTime")
    .build();
```

#### 8.4.3 ReadBuilder

实体类数据读取构建器。

**主要方法**：

| 方法 | 说明 | 返回类型 |
|------|------|----------|
| `ignoreUniqueFields(String... fields)` | 忽略指定唯一字段 | `ReadBuilder<T>` |
| `build()` | 构建并执行读取 | `List<T>` |

**示例**：
```java
List<Employee> employees = FsHelper.readBuilder(sheetId, spreadsheetToken, Employee.class)
    .ignoreUniqueFields("updateTime")
    .build();
```

#### 8.4.4 MapSheetBuilder

Map 方式表格创建构建器。

**主要方法**：与 `MapSheetConfig` 类似，支持链式调用。

**示例**：
```java
String sheetId = FsHelper.createMapSheetBuilder("表格", spreadsheetToken)
    .titleRow(2)
    .headLine(3)
    .addField(MapFieldDefinition.text("姓名", 0))
    .addField(MapFieldDefinition.text("邮箱", 1))
    .build();
```

#### 8.4.5 MapWriteBuilder

Map 方式数据写入构建器。

**主要方法**：与 `MapTableConfig` 类似，支持链式调用。

**示例**：
```java
FsHelper.writeMapBuilder(sheetId, spreadsheetToken, dataList)
    .titleRow(1)
    .headLine(2)
    .addUniKeyName("姓名")
    .enableCover(true)
    .build();
```

#### 8.4.6 MapReadBuilder

Map 方式数据读取构建器。

**主要方法**：

| 方法 | 说明 | 返回类型 |
|------|------|----------|
| `config(MapTableConfig)` | 设置配置 | `MapReadBuilder` |
| `titleRow(int)` | 设置标题行 | `MapReadBuilder` |
| `headLine(int)` | 设置数据起始行 | `MapReadBuilder` |
| `addUniKeyName(String)` | 添加唯一键 | `MapReadBuilder` |
| `build()` | 构建并执行读取 | `List<Map<String, Object>>` |
| `groupBuild()` | 构建并执行分组读取 | `Map<String, List<Map<String, Object>>>` |

**示例**：
```java
// 普通读取
List<Map<String, Object>> dataList = FsHelper.readMapBuilder(sheetId, spreadsheetToken)
    .titleRow(1)
    .headLine(2)
    .build();

// 分组读取
Map<String, List<Map<String, Object>>> groupedData = FsHelper.readMapBuilder(sheetId, spreadsheetToken)
    .titleRow(2)
    .headLine(3)
    .groupBuild();
```

---

## 9. 最佳实践

### 9.1 性能优化

#### 9.1.1 批量操作建议

- **批量写入**：尽量将多条数据合并为一次批量写入，而不是逐条写入
- **批量读取**：使用 `read()` 方法一次性读取所有需要的数据，而不是多次读取

**示例**：
```java
// ✅ 推荐：批量写入
List<Employee> employees = new ArrayList<>();
// ... 添加多条数据
FsHelper.write(sheetId, spreadsheetToken, employees);

// ❌ 不推荐：逐条写入
for (Employee emp : employees) {
    FsHelper.write(sheetId, spreadsheetToken, Collections.singletonList(emp));
}
```

#### 9.1.2 大数据量处理

对于数据量较大的表格（超过1000行），建议：

1. **分批处理**：将数据分批写入，每批处理 100-500 条数据
2. **异步处理**：使用异步方式处理，避免阻塞主线程
3. **使用 Upsert**：对于需要更新的场景，使用 Upsert 模式避免重复数据

**示例**：
```java
public void batchWrite(List<Employee> employees) {
    int batchSize = 500;
    int total = employees.size();
    
    for (int i = 0; i < total; i += batchSize) {
        int end = Math.min(i + batchSize, total);
        List<Employee> batch = employees.subList(i, end);
        
        FsHelper.write(sheetId, spreadsheetToken, batch);
        
        System.out.println("已处理: " + end + "/" + total);
    }
}
```

#### 9.1.3 连接管理

- **单例模式**：`FsClient.getInstance()` 返回单例，建议在应用启动时初始化一次
- **资源释放**：使用 try-with-resources 确保资源正确释放
- **连接池**：框架内部已实现连接池，无需手动管理

### 9.2 错误处理

#### 9.2.1 异常类型

框架可能抛出以下异常：

- **`FsHelperException`**：飞书表格操作异常
- **`TokenManagementException`**：Token 管理异常
- **`IllegalArgumentException`**：参数错误异常

#### 9.2.2 错误处理示例

```java
try (FsClient fsClient = FsClient.getInstance()) {
    fsClient.initializeClient("your_app_id", "your_app_secret");
    
    try {
        String sheetId = FsHelper.create("员工表", spreadsheetToken, Employee.class);
        FsHelper.write(sheetId, spreadsheetToken, employees);
    } catch (FsHelperException e) {
        // 处理飞书表格操作异常
        logger.error("飞书表格操作失败: " + e.getMessage(), e);
        // 根据业务需求决定是否重试或通知用户
    } catch (IllegalArgumentException e) {
        // 处理参数错误
        logger.error("参数错误: " + e.getMessage(), e);
    }
} catch (Exception e) {
    // 处理其他异常
    logger.error("初始化失败: " + e.getMessage(), e);
}
```

### 9.3 注意事项

#### 9.3.1 字段命名规范

- **实体类字段**：使用驼峰命名（如 `employeeId`, `userName`）
- **表格列名**：使用中文或英文，建议使用有意义的名称
- **唯一键字段**：确保唯一键字段在实体类和表格中的名称一致

#### 9.3.2 唯一键设计原则

- **选择合适的唯一键**：选择业务上真正唯一的字段作为唯一键
- **复合唯一键**：可以使用多个字段组合作为唯一键
- **避免使用时间戳**：时间戳可能重复，不适合作为唯一键

**示例**：
```java
// ✅ 推荐：使用业务唯一字段
@TableConf(uniKeys = {"employeeId"})

// ✅ 推荐：使用复合唯一键
@TableConf(uniKeys = {"orderId", "productId"})

// ❌ 不推荐：使用时间戳
@TableConf(uniKeys = {"createTime"})
```

#### 9.3.3 表头设计建议

- **单层级表头**：对于简单的表格，使用单层级表头即可
- **多层级表头**：对于复杂的报表，使用多层级表头提高可读性
- **字段顺序**：使用 `order` 参数控制字段顺序，确保逻辑清晰
- **字段描述**：使用 `desc` 参数为字段添加描述，帮助用户理解字段含义

#### 9.3.4 Upsert 模式使用建议

- **增量更新场景**：对于需要定期同步数据的场景，使用 Upsert 模式
- **避免重复数据**：Upsert 模式可以避免重复插入相同的数据
- **唯一键必须**：使用 Upsert 模式时，必须配置 `uniKeys`

#### 9.3.5 图片上传注意事项

- **图片格式**：支持常见的图片格式（JPG、PNG 等）
- **图片大小**：建议图片大小不超过 10MB
- **FileData 对象**：使用 `FileData` 对象封装图片信息

**示例**：
```java
FileData imageData = new FileData();
imageData.setFileName("avatar.jpg");
imageData.setImageData(imageBytes);  // 图片的字节数组
imageData.setFileType(FileType.IMAGE.getType());

Employee emp = new Employee();
emp.setName("张三");
emp.setAvatar(imageData);  // 设置图片字段

FsHelper.write(sheetId, spreadsheetToken, Collections.singletonList(emp));
```

---

## 10. 常见问题（FAQ）

### 10.1 初始化相关问题

#### Q1: 如何获取 App ID 和 App Secret？

**A**: 
1. 登录 [飞书开放平台](https://open.feishu.cn/)
2. 创建企业自建应用
3. 在应用详情页面获取 App ID 和 App Secret
4. 确保应用已开通"电子表格"相关权限

#### Q2: 初始化时提示 Token 获取失败？

**A**: 可能的原因：
- App ID 或 App Secret 错误
- 应用未开通相关权限
- 网络连接问题

**解决方案**：
- 检查 App ID 和 App Secret 是否正确
- 在飞书开放平台确认应用权限已开通
- 检查网络连接

### 10.2 数据读写相关问题

#### Q3: 写入数据后，表格中没有数据？

**A**: 可能的原因：
- `headLine` 配置错误，数据写到了错误的位置
- 字段名不匹配
- 唯一键配置错误，数据被更新到了其他行

**解决方案**：
- 检查 `headLine` 配置，确保数据写入到正确的位置
- 检查字段名是否与表格列名一致
- 检查唯一键配置是否正确

#### Q4: 读取数据时，某些字段为 null？

**A**: 可能的原因：
- 表格中该字段为空
- 字段名不匹配
- 字段类型不匹配

**解决方案**：
- 检查表格中该字段是否有值
- 检查 `@TableProperty` 注解中的 `value` 是否与表格列名一致
- 检查字段类型是否匹配

#### Q5: Upsert 模式不生效？

**A**: 可能的原因：
- 未配置 `uniKeys`
- `upsert` 设置为 `false`

**解决方案**：
- 确保在 `@TableConf` 中配置了 `uniKeys`
- 确保 `upsert` 设置为 `true`（默认值）

### 10.3 表格创建相关问题

#### Q6: 创建表格时，表头样式不正确？

**A**: 可能的原因：
- `headLine` 和 `titleRow` 配置错误
- 多层级表头配置不正确

**解决方案**：
- 检查 `headLine` 和 `titleRow` 的配置
- 对于多层级表头，确保 `value` 数组的长度和顺序正确

#### Q7: 如何创建带下拉选项的字段？

**A**: 使用 `TypeEnum.SINGLE_SELECT` 或 `TypeEnum.MULTI_SELECT`，并配置选项：

**注解方式**：
```java
@TableProperty(
    value = "状态",
    type = TypeEnum.SINGLE_SELECT,
    enumClass = StatusEnum.class  // 使用枚举类
)
private String status;
```

**Map 方式**：
```java
MapFieldDefinition.singleSelect("状态", 0, "启用", "禁用")
```

#### Q8: 如何创建多层级表头？

**A**: 在 `@TableProperty` 的 `value` 参数中使用数组：

```java
@TableProperty(value = {"基本信息", "姓名"}, order = 0)
private String name;

@TableProperty(value = {"基本信息", "邮箱"}, order = 1)
private String email;
```

相同第一层级的字段会自动合并表头。

### 10.4 配置相关问题

#### Q9: `headLine` 和 `titleRow` 的区别？

**A**: 
- `titleRow`: 标题行行号，表头所在的行（从1开始）
- `headLine`: 数据起始行行号，数据从这一行开始写入（从1开始）

**示例**：
- 如果表头在第1行，数据从第2行开始：`titleRow = 1, headLine = 2`
- 如果表头在第2-3行（多层级），数据从第4行开始：`titleRow = 3, headLine = 4`

#### Q10: 如何实现分组表格？

**A**: 使用 Map 方式创建分组表格：

```java
MapSheetConfig config = MapSheetConfig.sheetBuilder()
    .addField(MapFieldDefinition.text("姓名", 0))
    .addField(MapFieldDefinition.text("邮箱", 1))
    .addGroupField("技术部")
    .addGroupField("销售部")
    .build();

String sheetId = FsHelper.createMapSheet("分组表格", spreadsheetToken, config);
```

#### Q11: 如何自定义字段格式化？

**A**: 实现 `FieldValueProcess` 接口：

```java
public class CustomFieldProcessor implements FieldValueProcess<String> {
    @Override
    public String process(Object value) {
        // 自定义处理逻辑
        return value.toString().toUpperCase();
    }
    
    @Override
    public Object reverseProcess(Object value) {
        // 反向处理逻辑
        return value.toString().toLowerCase();
    }
}

// 在注解中使用
@TableProperty(
    value = "姓名",
    fieldFormatClass = CustomFieldProcessor.class
)
private String name;
```

#### Q12: 如何动态设置下拉选项？

**A**: 实现 `OptionsValueProcess` 接口：

```java
public class DynamicOptionsProcessor implements OptionsValueProcess<List<String>, Map<String, Object>> {
    @Override
    public List<String> process(Map<String, Object> properties) {
        // 从 properties 中获取字段信息
        FieldProperty field = (FieldProperty) properties.get("_field");
        
        // 动态生成选项列表
        List<String> options = new ArrayList<>();
        // ... 生成逻辑
        return options;
    }
}

// 在注解中使用
@TableProperty(
    value = "状态",
    type = TypeEnum.SINGLE_SELECT,
    optionsClass = DynamicOptionsProcessor.class
)
private String status;
```

---

## 附录

### A. 版本更新日志

#### v0.0.5
- 支持 Map 配置方式创建和操作表格
- 支持分组表格创建和读取
- 优化多层级表头合并逻辑
- 修复 Upsert 模式相关问题

### B. 相关资源

- [飞书开放平台文档](https://open.feishu.cn/document/)
- [项目 GitHub 仓库](https://github.com/luckday-cn/feishu-table-helper)
- [问题反馈](https://github.com/luckday-cn/feishu-table-helper/issues)

### C. 技术支持

如有问题或建议，欢迎：
- 提交 Issue
- 发送邮件至：luckday@isliu.cn

---

**文档版本**: 1.0  
**最后更新**: 2025-11-05  
**维护者**: feishu-table-helper 团队