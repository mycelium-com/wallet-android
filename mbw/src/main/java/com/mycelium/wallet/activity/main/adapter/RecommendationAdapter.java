package com.mycelium.wallet.activity.main.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.main.model.PartnerInfo;
import com.mycelium.wallet.activity.main.model.RecommendationBanner;
import com.mycelium.wallet.activity.main.model.RecommendationFooter;
import com.mycelium.wallet.activity.main.model.RecommendationHeader;
import com.mycelium.wallet.activity.main.model.RecommendationInfo;

import java.util.ArrayList;
import java.util.List;

public class RecommendationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<RecommendationInfo> partnerInfos = new ArrayList<>();
    private ClickListener clickListener;

    public RecommendationAdapter(List<RecommendationInfo> partnerInfos) {
        this.partnerInfos.addAll(partnerInfos);
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == PartnerInfo.PARTNER_TYPE) {
            return new PartnerHolder(inflater.inflate(R.layout.main_recommendations_list_item, parent, false));
        } else if (viewType == RecommendationFooter.FOOTER_TYPE) {
            return new FooterHolder(inflater.inflate(R.layout.main_recommendations_list_footer, parent, false));
        } else if (viewType == RecommendationBanner.BANNER_TYPE) {
            return new BannerHolder(inflater.inflate(R.layout.main_recommendations_list_banner, parent, false));
        } else if (viewType == RecommendationHeader.HEADER_TYPE) {
            return new HeaderHolder(inflater.inflate(R.layout.main_recommendations_list_header, parent, false));
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == PartnerInfo.PARTNER_TYPE) {
            PartnerHolder partnerHolder = (PartnerHolder) holder;
            final PartnerInfo bean = (PartnerInfo) partnerInfos.get(position);
            partnerHolder.txtName.setText(bean.getName());
            partnerHolder.txtDescription.setText(bean.getDescription());
            partnerHolder.imgIcon.setImageResource(bean.getIcon());

            partnerHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (clickListener != null) {
                        clickListener.onClick(bean);
                    }
                }
            });
        } else if (getItemViewType(position) == RecommendationFooter.FOOTER_TYPE) {
            FooterHolder footerHolder = (FooterHolder) holder;
            final RecommendationFooter footer = (RecommendationFooter) partnerInfos.get(position);
            footerHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (clickListener != null) {
                        clickListener.onClick(footer);
                    }
                }
            });
        } else if (getItemViewType(position) == RecommendationBanner.BANNER_TYPE) {
            BannerHolder bannerHolder = (BannerHolder) holder;
            final RecommendationBanner banner = (RecommendationBanner) partnerInfos.get(position);
            bannerHolder.imageView.setImageDrawable(banner.banner);
            bannerHolder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (clickListener != null) {
                        clickListener.onClick(banner);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return partnerInfos.size();
    }

    @Override
    public int getItemViewType(int position) {
        return partnerInfos.get(position).type;
    }

    class PartnerHolder extends RecyclerView.ViewHolder {
        public ImageView imgIcon;
        public TextView txtName;
        public TextView txtDescription;

        public PartnerHolder(View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.ivIcon);
            txtName = itemView.findViewById(R.id.tvTitle);
            txtDescription = itemView.findViewById(R.id.tvDescription);

        }
    }

    class FooterHolder extends RecyclerView.ViewHolder {
        public FooterHolder(View itemView) {
            super(itemView);
        }
    }

    class BannerHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public BannerHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.bannerImage);
        }
    }

    class HeaderHolder extends RecyclerView.ViewHolder {
        public HeaderHolder(View itemView) {
            super(itemView);
        }
    }

    public interface ClickListener {
        void onClick(PartnerInfo recommendationInfo);

        void onClick(RecommendationFooter recommendationFooter);

        void onClick(RecommendationBanner recommendationBanner);
    }
}
