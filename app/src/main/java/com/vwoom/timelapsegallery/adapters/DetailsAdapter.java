package com.vwoom.timelapsegallery.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vwoom.timelapsegallery.R;
import com.vwoom.timelapsegallery.database.entry.PhotoEntry;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailsAdapter extends RecyclerView.Adapter<DetailsAdapter.DetailsAdapterViewHolder> {

    private final static String TAG = DetailsAdapter.class.getSimpleName();

    private List<PhotoEntry> mPhotos;
    private final DetailsAdapterOnClickHandler mClickHandler;
    private PhotoEntry mCurrentPhoto;

    public interface DetailsAdapterOnClickHandler {
        void onClick(PhotoEntry clickedPhoto);
    }

    public DetailsAdapter(DetailsAdapterOnClickHandler handler){ mClickHandler = handler;}

    public class DetailsAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.detail_thumbnail) ImageView mDetailThumbnail;
        @BindView(R.id.selection_indicator) View mSelectionIndicator;

        public DetailsAdapterViewHolder(View view){
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int adapterPosition = getAdapterPosition();
            PhotoEntry clickedPhoto = mPhotos.get(adapterPosition);
            mCurrentPhoto = clickedPhoto;
            mClickHandler.onClick(clickedPhoto);
        }
    }

    @NonNull
    @Override
    public DetailsAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForGridItem = R.layout.detail_recyclerview_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;
        View view = inflater.inflate(layoutIdForGridItem, parent, shouldAttachToParentImmediately);
        return new DetailsAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DetailsAdapterViewHolder holder, int position) {
        PhotoEntry currentPhoto = mPhotos.get(position);
        String photo_path = currentPhoto.getUrl();
        File f = new File(photo_path);

        // TODO dynamically resize detail view?
        Glide.with(holder.itemView.getContext())
                .load(f)
                .centerCrop()
                .into(holder.mDetailThumbnail);

        if (mPhotos.get(position) == mCurrentPhoto){
            holder.mSelectionIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.mSelectionIndicator.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        if (mPhotos == null) return 0;
        return mPhotos.size();
    }

    public void setPhotoData(List<PhotoEntry> photoData){
        mPhotos = photoData;
        notifyDataSetChanged();
    }

    public void setCurrentPhoto(PhotoEntry photo){
        mCurrentPhoto = photo;
        notifyDataSetChanged();
    }
}
