package com.vachel.xpopwindow.sample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.vachel.xpopwindow.R;
import com.vachel.xpopwindow.XPopWindow;

public class MainActivity extends AppCompatActivity implements XPopWindow.IXPopupListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final RecyclerView recycleView = findViewById(R.id.recycle_view);
        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recycleView.setLayoutManager(manager);
        String[] labels = new String[36];
        for (int i = 0; i < 36; i++) {
            labels[i] = "item: "+ i ;
        }

        final String[] items = new String[]{
                "复制", "删除", "粘贴", "引用","收藏","保存", "转发"
        };
        final int[] icons = new int[]{
                R.mipmap.pop_icon_copy,
                R.mipmap.pop_icon_delete,
                R.mipmap.pop_icon_copy,
                R.mipmap.pop_icon_delete,
                R.mipmap.pop_icon_copy,
                R.mipmap.pop_icon_delete,
                R.mipmap.pop_icon_copy
        };
        MyAdapter myAdapter = new MyAdapter(labels, new OnItemClickListener() {
            @Override
            public void onItemLongClick(View view, String itemName) {
                Log.d("MainActivity", itemName);
                // 长按展示气泡
                XPopWindow.build(MainActivity.this, view)
                        .bindRecyclerView(recycleView)
                        .setItems(items)
                        .setIcons(icons)
                        .setDividerVerticalEnable(true)
                        .setDividerHorizontalEnable(false)
                        .setListener(MainActivity.this)
                        .show();

            }
        });
        recycleView.setAdapter(myAdapter);
    }

    @Override
    public void onPopupListClick(View contextView, String label) {
        Toast.makeText(this, label, Toast.LENGTH_SHORT).show();
    }

    static class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyHolder> {
        private String[] mList;
        private OnItemClickListener mListener;

        MyAdapter(String[] list, OnItemClickListener listener) {
            mList = list;
            mListener = listener;
        }

        @NonNull
        @Override
        public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_one, parent, false);
            return new MyHolder(view);
        }

        @Override
        public void onBindViewHolder(MyHolder holder, final int position) {
            holder.textView.setText(mList[position]);
            holder.textView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    mListener.onItemLongClick(view, mList[position]);
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return mList.length;
        }

        static class MyHolder extends RecyclerView.ViewHolder {

            TextView textView;

            MyHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.tv_content);
            }
        }
    }

    interface OnItemClickListener {
        void onItemLongClick(View view, String itemName);
    }
}
