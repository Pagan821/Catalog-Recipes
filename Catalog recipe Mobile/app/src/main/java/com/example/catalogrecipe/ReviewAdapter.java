package com.example.catalogrecipe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class ReviewAdapter extends ArrayAdapter<Review> {
    private Context context;
    private List<Review> reviews;
    private int currentUserId;
    private DatabaseHelper dbHelper;
    private OnReviewDeletedListener listener;

    public interface OnReviewDeletedListener {
        void onReviewDeleted();
    }

    public ReviewAdapter(Context context, List<Review> reviews, int currentUserId, OnReviewDeletedListener listener) {
        super(context, R.layout.item_review, reviews);
        this.context = context;
        this.reviews = reviews;
        this.currentUserId = currentUserId;
        this.dbHelper = new DatabaseHelper(context);
        this.listener = listener;
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
        Button btnDeleteReview = convertView.findViewById(R.id.btnDeleteReview);

        tvUsername.setText(review.getUsername());
        tvReview.setText(review.getReviewText());
        tvDate.setText(review.getCreatedAt());
        ratingBar.setRating(review.getRating());

        // Показываем кнопку удаления только для отзывов текущего пользователя
        if (review.getUserId() == currentUserId) {
            btnDeleteReview.setVisibility(View.VISIBLE);
            btnDeleteReview.setOnClickListener(v -> {
                // Удаляем отзыв
                if (dbHelper.deleteReview(review.getId(), review.getRecipeId())) {
                    reviews.remove(position);
                    notifyDataSetChanged();
                    Toast.makeText(context, "Отзыв удален", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onReviewDeleted();
                    }
                } else {
                    Toast.makeText(context, "Ошибка при удалении", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            btnDeleteReview.setVisibility(View.GONE);
        }

        return convertView;
    }

    @Override
    public int getCount() {
        return reviews.size();
    }
}