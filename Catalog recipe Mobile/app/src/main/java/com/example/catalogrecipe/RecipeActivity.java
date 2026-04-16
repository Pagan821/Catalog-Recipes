package com.example.catalogrecipe;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.List;

public class RecipeActivity extends AppCompatActivity {
    private ListView listView;
    private EditText etSearch;
    private Button btnSearch, btnShowAll;
    private Spinner spinnerFilter;
    private DatabaseHelper dbHelper;
    private List<Recipe> currentRecipes;
    private RecipeAdapter adapter;
    private int userId;
    private String username;
    private boolean isAdmin;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Bitmap selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        userId = getIntent().getIntExtra("user_id", -1);
        username = getIntent().getStringExtra("username");
        isAdmin = getIntent().getBooleanExtra("is_admin", false);
        dbHelper = new DatabaseHelper(this);

        listView = findViewById(R.id.listView);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnShowAll = findViewById(R.id.btnShowAll);
        spinnerFilter = findViewById(R.id.spinnerFilter);

        String[] filters = {"Все рецепты", "Мои рецепты"};
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filters);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);

        loadRecipes();

        btnSearch.setOnClickListener(v -> searchRecipes());
        btnShowAll.setOnClickListener(v -> loadRecipes());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(RecipeActivity.this, RecipeDetailActivity.class);
            intent.putExtra("recipe_id", currentRecipes.get(position).getId());
            intent.putExtra("user_id", userId);
            intent.putExtra("username", username);
            intent.putExtra("is_admin", isAdmin);
            startActivity(intent);
        });

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) loadRecipes();
                else loadMyRecipes();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(RecipeActivity.this, RecipeDetailActivity.class);
            intent.putExtra("recipe_id", currentRecipes.get(position).getId());
            intent.putExtra("user_id", userId);
            intent.putExtra("username", username);
            intent.putExtra("is_admin", isAdmin);
            startActivity(intent);
        });


        if (isAdmin) {
            listView.setOnItemLongClickListener((parent, view, position, id) -> {
                showDeleteDialog(currentRecipes.get(position));
                return true;
            });
        }
    }

    private void loadRecipes() {
        currentRecipes = dbHelper.getAllRecipes();
        adapter = new RecipeAdapter(this, currentRecipes);
        listView.setAdapter(adapter);
    }

    private void loadMyRecipes() {
        currentRecipes = dbHelper.getUserRecipes(userId);
        adapter = new RecipeAdapter(this, currentRecipes);
        listView.setAdapter(adapter);
    }

    private void searchRecipes() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            loadRecipes();
            return;
        }
        currentRecipes = dbHelper.searchRecipes(query);
        adapter = new RecipeAdapter(this, currentRecipes);
        listView.setAdapter(adapter);
        if (currentRecipes.isEmpty()) Toast.makeText(this, "Рецепты не найдены", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteDialog(Recipe recipe) {
        new AlertDialog.Builder(this)
                .setTitle("Удаление рецепта")
                .setMessage("Удалить \"" + recipe.getTitle() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    if (dbHelper.deleteRecipe(recipe.getId())) {
                        Toast.makeText(this, "Рецепт удален", Toast.LENGTH_SHORT).show();
                        loadRecipes();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (isAdmin) menu.findItem(R.id.action_admin_panel).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_add_recipe) {
            showAddRecipeDialog();
            return true;
        } else if (itemId == R.id.action_admin_panel && isAdmin) {
            showAdminPanel();
            return true;
        } else if (itemId == R.id.action_logout) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddRecipeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить рецепт");

        View view = getLayoutInflater().inflate(R.layout.dialog_add_recipe, null);
        EditText etTitle = view.findViewById(R.id.etTitle);
        EditText etDescription = view.findViewById(R.id.etDescription);
        EditText etIngredients = view.findViewById(R.id.etIngredients);
        EditText etInstructions = view.findViewById(R.id.etInstructions);
        ImageView ivImage = view.findViewById(R.id.ivImage);
        Button btnSelectImage = view.findViewById(R.id.btnSelectImage);

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

            Recipe recipe = new Recipe();
            recipe.setTitle(title);
            recipe.setDescription(description.isEmpty() ? "Нет описания" : description);
            recipe.setIngredients(ingredients);
            recipe.setInstructions(instructions);
            recipe.setAuthorId(userId);
            recipe.setImage(selectedImage);

            if (dbHelper.addRecipe(recipe)) {
                Toast.makeText(this, "Рецепт добавлен!", Toast.LENGTH_SHORT).show();
                loadRecipes();
                selectedImage = null;
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showAdminPanel() {
        List<Recipe> allRecipes = dbHelper.getAllRecipes();
        if (allRecipes.isEmpty()) {
            Toast.makeText(this, "Нет рецептов для управления", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] recipeNames = new String[allRecipes.size()];
        for (int i = 0; i < allRecipes.size(); i++) {
            recipeNames[i] = allRecipes.get(i).getTitle() + " (автор: " + allRecipes.get(i).getAuthorName() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Управление рецептами (нажмите для удаления)")
                .setItems(recipeNames, (dialog, which) -> {
                    Recipe recipe = allRecipes.get(which);
                    new AlertDialog.Builder(this)
                            .setTitle("Удалить рецепт?")
                            .setMessage(recipe.getTitle())
                            .setPositiveButton("Удалить", (d, w) -> {
                                if (dbHelper.deleteRecipe(recipe.getId())) {
                                    Toast.makeText(this, "Рецепт удален", Toast.LENGTH_SHORT).show();
                                    loadRecipes();
                                }
                            })
                            .setNegativeButton("Отмена", null)
                            .show();
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                ImageView ivImage = findViewById(R.id.ivImage);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecipes();
    }
}