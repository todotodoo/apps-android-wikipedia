package org.wikipedia.feed.news;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.view.ListCardItemView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.ViewUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.wikipedia.feed.news.NewsActivity.EXTRA_NEWS_ITEM;
import static org.wikipedia.feed.news.NewsActivity.EXTRA_WIKI;
import static org.wikipedia.richtext.RichTextUtil.stripHtml;
import static org.wikipedia.util.DimenUtil.newsFeatureImageHeightForDevice;

public class NewsFragment extends Fragment {
    @BindView(R.id.view_news_fullscreen_header_image) FaceAndColorDetectImageView image;
    @BindView(R.id.view_news_fullscreen_story_text) TextView text;
    @BindView(R.id.view_news_fullscreen_link_card_list) RecyclerView links;
    @BindView(R.id.view_news_fullscreen_toolbar) Toolbar toolbar;

    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private Unbinder unbinder;

    @NonNull
    public static NewsFragment newInstance(@NonNull NewsItem item, @NonNull WikiSite wiki) {
        NewsFragment instance = new NewsFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_NEWS_ITEM, GsonMarshaller.marshal(item));
        args.putString(EXTRA_WIKI, GsonMarshaller.marshal(wiki));
        instance.setArguments(args);
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_news, container, false);
        unbinder = ButterKnife.bind(this, view);

        ViewUtil.setTopPaddingDp(toolbar, (int) DimenUtil.getTranslucentStatusBarHeight(getContext()));
        ViewUtil.setBackgroundDrawable(toolbar, GradientUtil.getCubicGradient(
                ContextCompat.getColor(getContext(), R.color.lead_gradient_start), Gravity.TOP));
        getAppCompatActivity().setSupportActionBar(toolbar);
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getAppCompatActivity().getSupportActionBar().setTitle("");

        NewsItem item = GsonUnmarshaller.unmarshal(NewsItem.class, getActivity().getIntent().getStringExtra(EXTRA_NEWS_ITEM));
        WikiSite wiki = GsonUnmarshaller.unmarshal(WikiSite.class, getActivity().getIntent().getStringExtra(EXTRA_WIKI));

        Uri imageUri = item.featureImage();
        int height = imageUri == null ? DimenUtil.getContentTopOffsetPx(getContext()) : newsFeatureImageHeightForDevice();
        if (imageUri == null) {
            toolbar.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.actionbar_background));
        }
        DimenUtil.setViewHeight(image, height);
        image.loadImage(imageUri);
        text.setText(stripHtml(item.story()));
        initRecycler();
        links.setAdapter(new RecyclerAdapter(item.linkCards(wiki), new Callback()));
        return view;
    }

    @Override public void onDestroyView() {
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }

    private void initRecycler() {
        links.setLayoutManager(new LinearLayoutManager(getContext()));
        links.addItemDecoration(new DrawableItemDecoration(getContext(),
                ResourceUtil.getThemedAttributeId(getContext(), R.attr.list_separator_drawable), true));
        links.setNestedScrollingEnabled(false);
    }

    protected static class RecyclerAdapter extends DefaultRecyclerAdapter<NewsLinkCard, ListCardItemView> {
        @Nullable private Callback callback;

        RecyclerAdapter(@NonNull List<NewsLinkCard> items, @NonNull Callback callback) {
            super(items);
            this.callback = callback;
        }

        @Override public DefaultViewHolder<ListCardItemView> onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DefaultViewHolder<>(new ListCardItemView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(DefaultViewHolder<ListCardItemView> holder, int position) {
            NewsLinkCard card = item(position);
            holder.getView().setHistoryEntry(new HistoryEntry(card.pageTitle(), HistoryEntry.SOURCE_NEWS));
            holder.getView().setCallback(callback);
        }
    }

    private class Callback implements ListCardItemView.Callback {
        @Override
        public void onSelectPage(@NonNull HistoryEntry entry) {
            startActivity(PageActivity.newIntent(getContext(), entry, entry.getTitle()));
        }

        @Override
        public void onAddPageToList(@NonNull HistoryEntry entry) {
            bottomSheetPresenter.show(getChildFragmentManager(),
                    AddToReadingListDialog.newInstance(entry.getTitle(),
                            AddToReadingListDialog.InvokeSource.NEWS_ACTIVITY));
        }

        @Override
        public void onSharePage(@NonNull HistoryEntry entry) {
            ShareUtil.shareText(getActivity(), entry.getTitle());
        }
    }

}
