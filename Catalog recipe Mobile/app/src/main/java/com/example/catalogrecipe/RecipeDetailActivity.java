package com.example.catalogrecipe;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.List;

public class RecipeDetailActivity extends AppCompatActivity {
    private TextView tvTitle, tvAuthor, tvDescription, tvIngredients, tvInstructions, tvOverallRating;
    private ImageView ivImage;
    private Button btnDelete, btnEdit, btnAddReview;
    private ListView listViewReviews;
    private DatabaseHelper dbHelper;
    private Recipe currentRecipe;
    private int userId;
    private String username;
    private boolean isAdmin;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Bitmap newImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        int recipeId = getIntent().getIntExtra("recipe_id", -1);
        userId = getIntent().getIntExtra("user_id", -1);
        username = getIntent().getStringExtra("username");
        isAdmin = getIntent().getBooleanExtra("is_admin", false);

        dbHelper = new DatabaseHelper(this);

        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvDescription = findViewById(R.id.tvDescription);
        tvIngredients = findViewById(R.id.tvIngredients);
        tvInstructions = findViewById(R.id.tvInstructions);
        tvOverallRating = findViewById(R.id.tvOverallRating);
        ivImage = findViewById(R.id.ivImage);
        btnDelete = findViewById(R.id.btnDelete);
        btnEdit = findViewById(R.id.btnEdit);
        btnAddReview = findViewById(R.id.btnAddReview);
        listViewReviews = findViewById(R.id.listViewReviews);

        loadRecipe(recipeId);
        loadReviews();

        btnAddReview.setOnClickListener(v -> showAddReviewDialog());

        boolean isAuthor = dbHelper.isRecipeAuthor(recipeId, userId);

        if (isAdmin || isAuthor) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> showDeleteDialog());
        }

        if (isAuthor) {
            btnEdit.setVisibility(View.VISIBLE);
            btnEdit.setOnClickListener(v -> showEditDialog());
            btnAddReview.setEnabled(false);
            btnAddReview.setText("Автор не может оставить отзыв");
        }

        // Проверяем, оставлял ли пользователь уже отзыв
        if (!isAuthor && dbHelper.hasUserReviewed(recipeId, userId)) {
            btnAddReview.setEnabled(false);
            btnAddReview.setText("✓ Вы уже оставили отзыв");
        }
    }

    private void loadRecipe(int recipeId) {
        currentRecipe = dbHelper.getRecipeById(recipeId);

        if (currentRecipe != null) {
            tvTitle.setText(currentRecipe.getTitle());
            tvAuthor.setText("Автор: " + currentRecipe.getAuthorName());
            tvDescription.setText(currentRecipe.getDescription());
            tvIngredients.setText("Ингредиенты:\n" + currentRecipe.getIngredients());
            tvInstructions.setText("Приготовление:\n" + currentRecipe.getInstructions());

            if (currentRecipe.getRatingCount() > 0) {
                tvOverallRating.setText(String.format("⭐ Общий рейтинг: %.1f (%d отзывов)",
                        currentRecipe.getRating(), currentRecipe.getRatingCount()));
            } else {
                tvOverallRating.setText("⭐ Пока нет отзывов");
            }

            if (currentRecipe.getImage() != null) {
                ivImage.setImageBitmap(currentRecipe.getImage());
            }
        } else {
            Toast.makeText(this, "Рецепт не найден", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadReviews() {
        List<Review> reviews = dbHelper.getReviewsForRecipe(currentRecipe.getId());
        ReviewAdapter adapter = new ReviewAdapter(this, reviews);
        listViewReviews.setAdapter(adapter);

        setListViewHeightBasedOnItems(listViewReviews);
    }

    private void setListViewHeightBasedOnItems(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) return;

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    private void showAddReviewDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить отзыв");

        View view = getLayoutInflater().inflate(R.layout.dialog_add_review, null);
        EditText etReview = view.findViewById(R.id.etReview);
        RatingBar ratingBarReview = view.findViewById(R.id.ratingBarReview);

        builder.setView(view);
        builder.setPositiveButton("Отправить", (dialog, which) -> {
            String reviewText = etReview.getText().toString().trim();
            int reviewRating = (int) ratingBarReview.getRating();

            if (reviewText.isEmpty()) {
                Toast.makeText(this, "Напишите отзыв", Toast.LENGTH_SHORT).show();
                return;
            }

            if (reviewRating == 0) {
                Toast.makeText(this, "Поставьте оценку", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success = dbHelper.addReview(currentRecipe.getId(), userId, username, reviewText, reviewRating);
            if (success) {
                Toast.makeText(this, "Отзыв добавлен! Спасибо!", Toast.LENGTH_SHORT).show();
                btnAddReview.setEnabled(false);
                btnAddReview.setText("✓ Вы уже оставили отзыв");
                loadRecipe(currentRecipe.getId());
                loadReviews();
            } else {
                Toast.makeText(this, "Вы уже оставили отзыв или это ваш рецепт", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Редактировать рецепт");

        View view = getLayoutInflater().inflate(R.layout.dialog_edit_recipe, null);
        EditText etTitle = view.findViewById(R.id.etTitle);
        EditText etDescription = view.findViewById(R.id.etDescription);
        EditText etIngredients = view.findViewById(R.id.etIngredients);
        EditText etInstructions = view.findViewById(R.id.etInstructions);
        ImageView ivImageEdit = view.findViewById(R.id.ivImage);
        Button btnSelectImage = view.findViewById(R.id.btnSelectImage);

        etTitle.setText(currentRecipe.getTitle());
        etDescription.setText(currentRecipe.getDescription());
        etIngredients.setText(currentRecipe.getIngredients());
        etInstructions.setText(currentRecipe.getInstructions());
        if (currentRecipe.getImage() != null) {
            ivImageEdit.setImageBitmap(currentRecipe.getImage());
        }

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Выберите фото"), PICK_IMAGE_REQUEST);
        });

        builder.setView(view);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String ingredients = etIngredients.getText().toString().trim();
            String instructions = etInstructions.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (title.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
                Toast.makeText(this, "Заполните название, ингредиенты и инструкцию", Toast.LENGTH_SHORT).show();
                return;
            }

            currentRecipe.setTitle(title);
            currentRecipe.setDescription(description.isEmpty() ? "Нет описания" : description);
            currentRecipe.setIngredients(ingredients);
            currentRecipe.setInstructions(instructions);
            if (newImage != null) {
                currentRecipe.setImage(newImage);
            }

            if (dbHelper.updateRecipe(currentRecipe)) {
                Toast.makeText(this, "Рецепт обновлен!", Toast.LENGTH_SHORT).show();
                loadRecipe(currentRecipe.getId());
                newImage = null;
            } else {
                Toast.makeText(this, "Ошибка при обновлении", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                newImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}