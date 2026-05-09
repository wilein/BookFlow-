from pathlib import Path
from uuid import uuid4
import xml.etree.ElementTree as ET


OUT = Path(__file__).resolve().parent / "BookFlow_all_flowcharts.drawio"


PAGE_ATTRS = {
    "dx": "1600",
    "dy": "1000",
    "grid": "1",
    "gridSize": "10",
    "guides": "1",
    "tooltips": "1",
    "connect": "1",
    "arrows": "1",
    "fold": "1",
    "page": "1",
    "pageScale": "1",
    "pageWidth": "1600",
    "pageHeight": "1000",
    "math": "0",
    "shadow": "0",
}

BASE_TEXT = "whiteSpace=wrap;html=1;fontSize=14;fontFamily=Microsoft YaHei;align=center;verticalAlign=middle;strokeWidth=2;"
TERM_STYLE = "rounded=1;arcSize=50;fillColor=#E8F3FF;strokeColor=#2F6FB0;" + BASE_TEXT
PROC_STYLE = "rounded=0;fillColor=#FFF4E3;strokeColor=#B56A00;" + BASE_TEXT
PROC_GREEN_STYLE = "rounded=0;fillColor=#EAF7EA;strokeColor=#3A7D44;" + BASE_TEXT
PROC_PURPLE_STYLE = "rounded=0;fillColor=#F3ECFF;strokeColor=#7154A8;" + BASE_TEXT
PROC_GRAY_STYLE = "rounded=0;fillColor=#F5F5F5;strokeColor=#666666;" + BASE_TEXT
DECISION_STYLE = "rhombus;perimeter=rhombusPerimeter;fillColor=#FFF0F0;strokeColor=#B84646;" + BASE_TEXT
EDGE_STYLE = "edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=block;endFill=1;strokeWidth=2;strokeColor=#111111;"
TITLE_STYLE = "text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=24;fontStyle=1;fontFamily=Microsoft YaHei;"


class PageBuilder:
    def __init__(self, diagram, title):
        self.diagram = diagram
        self.model = ET.SubElement(diagram, "mxGraphModel", PAGE_ATTRS)
        self.root = ET.SubElement(self.model, "root")
        ET.SubElement(self.root, "mxCell", {"id": "0"})
        ET.SubElement(self.root, "mxCell", {"id": "1", "parent": "0"})
        self.counter = 1
        self.add_vertex(title, 520, 20, 560, 40, TITLE_STYLE, value=title)

    def next_id(self):
        self.counter += 1
        return f"n{self.counter}"

    def add_vertex(self, name, x, y, w, h, style, value=None):
        cell_id = self.next_id()
        cell = ET.SubElement(
            self.root,
            "mxCell",
            {
                "id": cell_id,
                "value": value if value is not None else name,
                "style": style,
                "vertex": "1",
                "parent": "1",
            },
        )
        ET.SubElement(cell, "mxGeometry", {"x": str(x), "y": str(y), "width": str(w), "height": str(h), "as": "geometry"})
        return cell_id

    def add_edge(self, source, target, value="", points=None):
        cell_id = self.next_id()
        edge = ET.SubElement(
            self.root,
            "mxCell",
            {
                "id": cell_id,
                "value": value,
                "style": EDGE_STYLE,
                "edge": "1",
                "parent": "1",
                "source": source,
                "target": target,
            },
        )
        geo = ET.SubElement(edge, "mxGeometry", {"relative": "1", "as": "geometry"})
        if points:
            arr = ET.SubElement(geo, "Array", {"as": "points"})
            for x, y in points:
                ET.SubElement(arr, "mxPoint", {"x": str(x), "y": str(y)})
        return cell_id


def build_login(page):
    s = page.add_vertex("开始", 90, 220, 150, 60, TERM_STYLE)
    a = page.add_vertex("获取微信登录凭证", 300, 220, 190, 60, PROC_STYLE)
    b = page.add_vertex("调用 /user/auth/wechat", 560, 220, 220, 60, PROC_STYLE)
    c = page.add_vertex("用户是否已存在", 860, 200, 180, 100, DECISION_STYLE)
    d = page.add_vertex("初始化用户记录", 1120, 90, 190, 60, PROC_STYLE)
    e = page.add_vertex("生成 Token 并返回用户信息", 1120, 300, 240, 60, PROC_STYLE)
    f = page.add_vertex("结束", 1430, 300, 150, 60, TERM_STYLE)
    page.add_edge(s, a)
    page.add_edge(a, b)
    page.add_edge(b, c)
    page.add_edge(c, d, "否")
    page.add_edge(c, e, "是")
    page.add_edge(d, e)
    page.add_edge(e, f)


def build_order(page):
    s = page.add_vertex("开始", 60, 240, 150, 60, TERM_STYLE)
    a = page.add_vertex("浏览书籍详情", 270, 240, 180, 60, PROC_STYLE)
    b = page.add_vertex("提交 /order/create", 520, 240, 190, 60, PROC_STYLE)
    c = page.add_vertex("书籍是否可下单", 800, 220, 180, 100, DECISION_STYLE)
    d = page.add_vertex("创建待支付订单", 1060, 120, 180, 60, PROC_STYLE)
    e = page.add_vertex("提示书籍已售出或不可交易", 1030, 360, 240, 60, PROC_GRAY_STYLE)
    f = page.add_vertex("模拟支付 /order/pay/mock", 1320, 120, 210, 60, PROC_STYLE)
    g = page.add_vertex("支付是否成功", 1590, 100, 180, 100, DECISION_STYLE)
    h = page.add_vertex("卖家发货 /order/ship", 1330, 280, 190, 60, PROC_STYLE)
    i = page.add_vertex("买家确认收货 /order/confirm-receipt", 1590, 280, 250, 60, PROC_STYLE)
    j = page.add_vertex("更新订单状态为已完成", 1370, 420, 220, 60, PROC_STYLE)
    k = page.add_vertex("结束", 1660, 420, 150, 60, TERM_STYLE)
    page.add_edge(s, a)
    page.add_edge(a, b)
    page.add_edge(b, c)
    page.add_edge(c, d, "是")
    page.add_edge(c, e, "否")
    page.add_edge(d, f)
    page.add_edge(f, g)
    page.add_edge(g, h, "是")
    page.add_edge(g, e, "否", points=[(1680, 250), (1150, 250), (1150, 390)])
    page.add_edge(h, i)
    page.add_edge(i, j)
    page.add_edge(j, k)
    page.add_edge(e, k, "", points=[(1150, 520), (1735, 520)])


def build_annotation(page):
    s = page.add_vertex("开始", 80, 260, 150, 60, TERM_STYLE)
    a = page.add_vertex("进入书籍详情页", 290, 260, 180, 60, PROC_GREEN_STYLE)
    b = page.add_vertex("请求 /annotation/list 查看批注", 540, 260, 250, 60, PROC_GREEN_STYLE)
    c = page.add_vertex("是否新增批注", 880, 240, 180, 100, DECISION_STYLE)
    d = page.add_vertex("结束浏览", 1140, 360, 160, 60, PROC_GRAY_STYLE)
    e = page.add_vertex("填写文字或图片批注", 1130, 120, 200, 60, PROC_GREEN_STYLE)
    f = page.add_vertex("如有图片先调用 /annotation/upload-image", 1400, 120, 270, 60, PROC_GREEN_STYLE)
    g = page.add_vertex("调用 /annotation/create 保存批注", 1730, 120, 250, 60, PROC_GREEN_STYLE)
    h = page.add_vertex("重新加载批注列表并展示", 1450, 300, 220, 60, PROC_GREEN_STYLE)
    i = page.add_vertex("结束", 1740, 300, 150, 60, TERM_STYLE)
    page.add_edge(s, a)
    page.add_edge(a, b)
    page.add_edge(b, c)
    page.add_edge(c, d, "否")
    page.add_edge(c, e, "是")
    page.add_edge(e, f)
    page.add_edge(f, g)
    page.add_edge(g, h)
    page.add_edge(h, i)
    page.add_edge(d, i, "", points=[(1220, 460), (1815, 460)])


def build_path(page):
    s = page.add_vertex("开始", 70, 240, 150, 60, TERM_STYLE)
    a = page.add_vertex("创建学习路径草稿 /path/save-draft", 290, 240, 250, 60, PROC_PURPLE_STYLE)
    b = page.add_vertex("维护路径节点（parent_id）", 620, 240, 220, 60, PROC_PURPLE_STYLE)
    c = page.add_vertex("绑定资源文件", 920, 240, 170, 60, PROC_PURPLE_STYLE)
    d = page.add_vertex("是否发布路径", 1180, 220, 180, 100, DECISION_STYLE)
    e = page.add_vertex("继续保存草稿并编辑", 1450, 360, 200, 60, PROC_GRAY_STYLE)
    f = page.add_vertex("调用 /path/publish 发布路径", 1450, 120, 220, 60, PROC_PURPLE_STYLE)
    g = page.add_vertex("开始学习 /path/progress/start", 1730, 120, 220, 60, PROC_PURPLE_STYLE)
    h = page.add_vertex("完成节点 /path/progress/complete-node", 1730, 260, 260, 60, PROC_PURPLE_STYLE)
    i = page.add_vertex("更新学习进度", 1730, 400, 180, 60, PROC_PURPLE_STYLE)
    j = page.add_vertex("结束", 1980, 400, 150, 60, TERM_STYLE)
    page.add_edge(s, a)
    page.add_edge(a, b)
    page.add_edge(b, c)
    page.add_edge(c, d)
    page.add_edge(d, f, "是")
    page.add_edge(d, e, "否")
    page.add_edge(f, g)
    page.add_edge(g, h)
    page.add_edge(h, i)
    page.add_edge(i, j)
    page.add_edge(e, b, "", points=[(1550, 540), (730, 540), (730, 300)])


def build_test(page):
    s = page.add_vertex("开始", 80, 260, 150, 60, TERM_STYLE)
    a = page.add_vertex("搭建测试环境", 290, 260, 170, 60, PROC_GRAY_STYLE)
    b = page.add_vertex("准备测试数据", 530, 260, 170, 60, PROC_GRAY_STYLE)
    c = page.add_vertex("执行功能测试", 770, 260, 170, 60, PROC_GRAY_STYLE)
    d = page.add_vertex("执行接口测试", 1010, 260, 170, 60, PROC_GRAY_STYLE)
    e = page.add_vertex("测试是否通过", 1280, 240, 180, 100, DECISION_STYLE)
    f = page.add_vertex("记录结果并归档", 1560, 140, 180, 60, PROC_GRAY_STYLE)
    g = page.add_vertex("记录缺陷并修复", 1560, 360, 180, 60, PROC_GRAY_STYLE)
    h = page.add_vertex("回归测试", 1820, 360, 150, 60, PROC_GRAY_STYLE)
    i = page.add_vertex("结束", 1820, 140, 150, 60, TERM_STYLE)
    page.add_edge(s, a)
    page.add_edge(a, b)
    page.add_edge(b, c)
    page.add_edge(c, d)
    page.add_edge(d, e)
    page.add_edge(e, f, "是")
    page.add_edge(e, g, "否")
    page.add_edge(f, i)
    page.add_edge(g, h)
    page.add_edge(h, c, "", points=[(1895, 520), (855, 520), (855, 320)])


def prettify(root):
    raw = ET.tostring(root, encoding="utf-8")
    try:
        import xml.dom.minidom as minidom
        return minidom.parseString(raw).toprettyxml(indent="  ", encoding="utf-8").decode("utf-8")
    except Exception:
        return raw.decode("utf-8")


def main():
    mxfile = ET.Element("mxfile", {"host": "app.diagrams.net", "version": "24.7.17", "type": "device"})

    pages = [
        ("图 4-3 用户登录流程图", build_login),
        ("图 4-4 书籍交易流程图", build_order),
        ("图 4-5 批注传承流程图", build_annotation),
        ("图 4-6 学习路径流程图", build_path),
        ("图 6-1 系统测试流程图", build_test),
    ]

    for name, builder in pages:
        diagram = ET.SubElement(mxfile, "diagram", {"id": uuid4().hex[:8], "name": name})
        page = PageBuilder(diagram, name)
        builder(page)

    OUT.write_text(prettify(mxfile), encoding="utf-8")
    print(OUT)


if __name__ == "__main__":
    main()
