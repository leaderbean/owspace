package com.wwb.dandu.view.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.wwb.dandu.R;
import com.wwb.dandu.app.App;
import com.wwb.dandu.di.components.DaggerArtComponent;
import com.wwb.dandu.di.modules.ArtModule;
import com.wwb.dandu.model.entity.Item;
import com.wwb.dandu.presenter.ArticalContract;
import com.wwb.dandu.presenter.ArticalPresenter;
import com.wwb.dandu.util.AppUtil;
import com.wwb.dandu.view.adapter.ArtRecycleViewAdapter;
import com.wwb.dandu.view.widget.CustomPtrHeader;
import com.wwb.dandu.view.widget.DividerItemDecoration;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;

public class ArtActivity extends BaseActivity implements ArticalContract.View {
    @Bind(R.id.title)
    TextView title;
    @Bind(R.id.toolBar)
    Toolbar toolbar;
    @Bind(R.id.recycleView)
    RecyclerView recycleView;
    @Bind(R.id.ptrFrameLayout)
    PtrClassicFrameLayout mPtrFrame;
    @Inject
    ArticalPresenter presenter;
    private ArtRecycleViewAdapter recycleViewAdapter;
    private int page = 1;
    private int mode = 1;
    private boolean isRefresh;
    private boolean hasMore=true;
    private String deviceId;
    private int lastVisibleItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_art);
        ButterKnife.bind(this);
        mode = getIntent().getIntExtra("mode", 1);
        initPresenter();
        initView();
    }
    private void initPresenter(){
        DaggerArtComponent.builder()
                .artModule(new ArtModule(this))
                .netComponent(App.get(this).getNetComponent())
                .build()
                .inject(this);
    }
    private void initView() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("");
        String tt = getIntent().getStringExtra("title");
        title.setText(tt);
        deviceId = AppUtil.getDeviceId(this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        recycleViewAdapter = new ArtRecycleViewAdapter(this);
        recycleView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recycleView.addItemDecoration(new DividerItemDecoration(this));
        recycleView.setAdapter(recycleViewAdapter);
        mPtrFrame.setPtrHandler(new PtrDefaultHandler() {
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                page=1;
                isRefresh=true;
                hasMore = true;
                loadData(page, mode, "0", deviceId, "0");
            }
        });
        mPtrFrame.setOffsetToRefresh(200);
        mPtrFrame.autoRefresh(true);
        CustomPtrHeader header = new CustomPtrHeader(this,mode);
        mPtrFrame.setHeaderView(header);
        mPtrFrame.addPtrUIHandler(header);
        recycleView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && !isRefresh && hasMore && (lastVisibleItem+1  == recycleViewAdapter.getItemCount())){
                    loadData(page, mode, recycleViewAdapter.getLastItemId(),deviceId, recycleViewAdapter.getLastItemCreateTime());
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                lastVisibleItem = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
            }
        });
    }
    private void loadData(int page, int mode, String pageId, String deviceId, String createTime) {
        presenter.getListByPage(page, mode, pageId, deviceId, createTime);
    }
    @Override
    public void showLoading() {

    }

    @Override
    public void dismissLoading() {

    }

    @Override
    public void showNoData() {

    }

    @Override
    public void showNoMore() {
        hasMore = false;
        if (!isRefresh){
            //显示没有更多
            recycleViewAdapter.setHasMore(false);
            recycleViewAdapter.notifyItemChanged(recycleViewAdapter.getItemCount()-1);
        }
    }

    @Override
    public void updateListUI(List<Item> itemList) {
        mPtrFrame.refreshComplete();
        page++;
        if (isRefresh) {
            recycleViewAdapter.setHasMore(true);
            recycleViewAdapter.setError(false);
            isRefresh = false;
            recycleViewAdapter.replaceAllData(itemList);
        } else {
            recycleViewAdapter.setArtList(itemList);
        }
    }

    @Override
    public void showOnFailure() {
        if (!isRefresh){
            //显示失败
            recycleViewAdapter.setError(true);
            recycleViewAdapter.notifyItemChanged(recycleViewAdapter.getItemCount()-1);
        }else{
            Toast.makeText(this,"~~~~(>_<)~~~~刷新失败",Toast.LENGTH_SHORT).show();
        }
    }
}
