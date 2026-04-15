package com.example.catalogrecipe;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class RecipeDetailActivity extends AppCompatActivity {
    private TextView tvTitle, tvAuthor, tvDescription, tvIngredients, tvInstructions;
    private ImageView ivImage;
    private RatingBar ratingBar;
    private Button btnRate, btnDelete;
    private DatabaseHelper dbHelper;
    private Recipe currentRecipe;
    private int userId;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        int recipeId = getIntent().getIntExtra("recipe_id", -1);
        userId = getIntent().getIntExtra("user_id", -1);
        isAdmin = getIntent().getBooleanExtra("is_admin", false);

        dbHelper = new DatabaseHelper(this);

        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvDescription = findViewById(R.id.tvDescription);
        tvIngredients = findViewById(R.id.tvIngredients);
        tvInstructions = findViewById(R.id.tvInstructions);
        ivImage = findViewById(R.id.ivImage);
        ratingBar = findViewById(R.id.ratingBar);
        btnRate = findViewById(R.id.btnRate);
        btnDelete = findViewById(R.id.btnDelete);

        // Загружаем рецепт
        for (Recipe r : dbHelper.getAllRecipes()) {
            if (r.getId() == recipeId) {
                currentRecipe = r;
                break;
            }
        }

        if (currentRecipe != null) {
            tvTitle.setText(currentRecipe.getTitle());
            tvAuthor.setText("Автор: " + currentRecipe.getAuthorName());
            tvDescription.setText(currentRecipe.getDescription());
            tvIngredients.setText("Ингредиенты:\n" + currentRecipe.getIngredients());
            tvInstructions.setText("Приготовление:\n" + currentRecipe.getInstructions());
            ratingBar.setRating(currentRecipe.getRating());
            if (currentRecipe.getImage() != null) ivImage.setImageBitmap(currentRecipe.getImage());

            // Проверяем, оценивал ли пользователь
            if (dbHelper.hasUserRatedRecipe(recipeId, userId)) {
                btnRate.setEnabled(false);
                btnRate.setText("✓ Вы уже оценили");
            }
        } else {
            Toast.makeText(this, "Рецепт не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnRate.setOnClickListener(v -> rateRecipe());

        if (isAdmin) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> showDeleteDialog());
        }
    }

    private void rateRecipe() {
        float rating = ratingBar.getRating();
        if (rating == 0) {
            Toast.makeText(this, "Поставьте оценку", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dbHelper.rateRecipe(currentRecipe.getId(), userId, (int) rating)) {
            Toast.makeText(this, "Спасибо за оценку! (" + (int)rating + "★)", Toast.LENGTH_SHORT).show();
            btnRate.setEnabled(false);
            btnRate.setText("✓ Вы уже оценили");
            // Обновляем рейтинг
            for (Recipe r : dbHelper.getAllRecipes()) {
                if (r.getId() == currentRecipe.getId()) {
                    ratingBar.setRating(r.getRating());
                    break;
                }
            }
        } else {
            Toast.makeText(this, "Вы уже оценили этот рецепт", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Удаление рецепта")
                .setMessage("Удалить \"" + currentRecipe.getTitle() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    if (dbHelper.deleteRecipe(currentRecipe.getId())) {
                        Toast.makeText(this, "Рецепт удален", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}