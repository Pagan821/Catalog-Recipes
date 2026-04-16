package com.example.catalogrecipe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.TextView;
import java.util.List;

public class ReviewAdapter extends ArrayAdapter<Review> {
    private Context context;
    private List<Review> reviews;

    public ReviewAdapter(Context context, List<Review> reviews) {
        super(context, R.layout.item_review, reviews);
        this.context = context;
        this.reviews = reviews;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        }

        Review review = reviews.get(position);

        TextView tvUsername = convertView.findViewById(R.id.tvUsername);
        TextView tvReview = convertView.findViewById(R.id.tvReview);
        TextView tvDate = convertView.findViewById(R.id.tvDate);
        RatingBar ratingBar = convertView.findViewById(R.id.ratingBar);

        tvUsername.setText(review.getUsername());
        tvReview.setText(review.getReviewText());
        tvDate.setText(review.getCreatedAt());
        ratingBar.setRating(review.getRating());

        return convertView;
    }

    @Override
    public int getCount() {
        return reviews.size();
    }
}