package com.vwoom.timelapsegallery.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vwoom.timelapsegallery.R;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.utils.PhotoUtils;
import com.vwoom.timelapsegallery.utils.TimeUtils;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProjectsAdapter extends RecyclerView.Adapter<ProjectsAdapter.ProjectsAdapterViewHolder> {

    private final static String TAG = ProjectsAdapter.class.getSimpleName();

    private List<ProjectEntry> mProjectData;
    private final ProjectsAdapterOnClickHandler mClickHandler;

    private ConstraintSet constraintSet = new ConstraintSet();

    public interface ProjectsAdapterOnClickHandler {
        void onClick(ProjectEntry clickedProject, View sharedElement, String transitionName, int position);
    }

    public ProjectsAdapter(ProjectsAdapterOnClickHandler handler){ mClickHandler = handler;}

    public class ProjectsAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.project_image) ImageView mProjectImageView;
        @BindView(R.id.project_name) TextView mProjectNameTextView;
        @BindView(R.id.project_recyclerview_constraint_layout) ConstraintLayout mConstraintLayout;
        @BindView(R.id.schedule_indicator) ImageView mScheduleIndicator;
        @BindView(R.id.next_submission_time_textview) TextView mNextTimeIndicator;
        @BindView(R.id.next_submission_day_countdown_textview) TextView mDayCountdownIndicator;
        @BindView(R.id.project_card_view) CardView mCardView;
        @BindView(R.id.project_image_gradient_overlay) View mGradientOverlay;

        public ProjectsAdapterViewHolder(View view){
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int adapterPosition = getAdapterPosition();
            ProjectEntry clickedProject = mProjectData.get(adapterPosition);
            String transitionName = mProjectImageView.getTransitionName();
            mClickHandler.onClick(clickedProject, mProjectImageView, transitionName, adapterPosition);
        }
    }

    @NonNull
    @Override
    public ProjectsAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForGridItem = R.layout.project_recyclerview_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;
        View view = inflater.inflate(layoutIdForGridItem, parent, shouldAttachToParentImmediately);
        return new ProjectsAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectsAdapterViewHolder holder, int position) {
        // Get project information
        ProjectEntry currentProject = mProjectData.get(position);
        String thumbnail_path = currentProject.getThumbnail_url();
        String projectName = currentProject.getName();

        // Set the name
        holder.mProjectNameTextView.setText(projectName);

        // Set the constraint ratio
        String ratio = PhotoUtils.getAspectRatioFromImagePath(thumbnail_path);
        if (ratio==null) ratio = holder.itemView.getContext().getString(R.string.default_aspect_ratio);
        constraintSet.clone(holder.mConstraintLayout);
        constraintSet.setDimensionRatio(holder.mProjectImageView.getId(), ratio);
        constraintSet.applyTo(holder.mConstraintLayout);

        // Set to original color so that views recycle appropriately
        holder.mConstraintLayout.setBackgroundColor(ContextCompat
                .getColor(holder.itemView.getContext(), R.color.colorLightPrimary));

        // Display schedule information
        if (currentProject.getSchedule() !=0) {
            // Get the next submisison
            long next = currentProject.getSchedule_next_submission();

            String dayCountdown;

            // Handle projects scheduled for today
            // OR projects that are past submission
            if (DateUtils.isToday(next) || System.currentTimeMillis() > next){
                dayCountdown = holder.itemView.getContext().getString(R.string.today);

                // Set color to red for todays projects
                holder.mConstraintLayout.setBackgroundColor(ContextCompat
                        .getColor(holder.itemView.getContext(), R.color.colorSubtlePinkAccent));
            }
            // Otherwise handle projects scheduled for later
            else {
                // Calculate day countdown
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(System.currentTimeMillis());
                int currentDay = cal.get(Calendar.DAY_OF_YEAR);
                cal.setTimeInMillis(next);
                int scheduledDay = cal.get(Calendar.DAY_OF_YEAR);
                int daysUntilPhoto = scheduledDay - currentDay;
                if (daysUntilPhoto == 1)
                    dayCountdown = holder.itemView.getContext().getString(R.string.one_day);
                else
                    dayCountdown = holder.itemView.getContext().getString(R.string.number_of_days, daysUntilPhoto);
            }

            // Get time of submission
            String time = TimeUtils.getTimeFromTimestamp(next);

            // Set fields
            holder.mNextTimeIndicator.setText(time);
            holder.mDayCountdownIndicator.setText(dayCountdown);

            // Set visibility
            holder.mScheduleIndicator.setVisibility(View.VISIBLE);
            holder.mDayCountdownIndicator.setVisibility(View.VISIBLE);
            holder.mNextTimeIndicator.setVisibility(View.VISIBLE);
            holder.mGradientOverlay.setVisibility(View.VISIBLE);
        }
        // If there is no schedule hide schedule information
        else {
            holder.mScheduleIndicator.setVisibility(View.INVISIBLE);
            holder.mNextTimeIndicator.setVisibility(View.INVISIBLE);
            holder.mDayCountdownIndicator.setVisibility(View.INVISIBLE);
            holder.mGradientOverlay.setVisibility(View.INVISIBLE);
        }

        // Set the transition name
        String transitionName = currentProject.getId() + currentProject.getName();
        holder.mProjectImageView.setTransitionName(transitionName);
        holder.mProjectImageView.setTag(R.string.transition_tag, currentProject.getThumbnail_url());

        // Load the image
        File f = new File(thumbnail_path);
        Glide.with(holder.itemView.getContext())
                .load(f)
                .into(holder.mProjectImageView);
    }

    @Override
    public int getItemCount() {
        if (mProjectData == null) return 0;
        return mProjectData.size();
    }

    public void setProjectData(List<ProjectEntry> projectData){
        mProjectData = projectData;
        notifyDataSetChanged();
    }
}
