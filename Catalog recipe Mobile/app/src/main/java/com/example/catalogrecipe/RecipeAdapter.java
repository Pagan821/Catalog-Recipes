package com.example.catalogrecipe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import java.util.List;

public class RecipeAdapter extends ArrayAdapter<Recipe> {
    private Context context;
    private List<Recipe> recipes;

    public RecipeAdapter(Context context, List<Recipe> recipes) {
        super(context, R.layout.item_recipe, recipes);
        this.context = context;
        this.recipes = recipes;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
        }

        Recipe recipe = recipes.get(position);

        ImageView ivRecipeImage = convertView.findViewById(R.id.ivRecipeImage);
        TextView tvTitle = convertView.findViewById(R.id.tvTitle);
        TextView tvAuthor = convertView.findViewById(R.id.tvAuthor);
        TextView tvDescription = convertView.findViewById(R.id.tvDescription);
        RatingBar ratingBar = convertView.findViewById(R.id.ratingBar);
        TextView tvRatingCount = convertView.findViewById(R.id.tvRatingCount);

        if (recipe.getImage() != null) {
            ivRecipeImage.setImageBitmap(recipe.getImage());
        } else {
            ivRecipeImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        tvTitle.setText(recipe.getTitle());
        tvAuthor.setText("Автор: " + recipe.getAuthorName());
        tvDescription.setText(recipe.getDescription());
        ratingBar.setRating(recipe.getRating());
        tvRatingCount.setText("(" + recipe.getRatingCount() + ")");

        return convertView;
    }
}