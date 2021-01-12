# XPopWindow
仿QQ微信按压弹窗；支持分割线(仿QQ雕刻线样式)，图标；绑定RecyclerView后滚动会重定位

### 用法
      XPopWindow.build(context, view) // view决定了展示位置，对齐view中点，优先展示在上方，空间不足在下方
                        .bindRecyclerView(recycleView) // 绑定recycleView（可不绑），recycleView滚动后弹窗位置会重新定位
                        .bindLifeCycle(lifecycleOwner) // 绑定生命周期（可防止activity意外中止导致popwindow接收不到dismiss）
                        .setItems(items) // 必须设置 弹窗的item；个数大于5个时分多列展示
                        .setIcons(icons) // items对应的图标，可以不设置
                        .setDividerVerticalEnable(true) // item间的雕刻线分割线
                        .setDividerHorizontalEnable(false)
                        .setListener(MainActivity.this)
                        .show();

### ![avatar](https://github.com/vachelx/XPopWindow/blob/main/20201229111949.png)