package com.example.catalogrecipe;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "recipes.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_RECIPES = "recipes";
    private static final String TABLE_REVIEWS = "reviews";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Таблица пользователей
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE, password TEXT, is_admin INTEGER DEFAULT 0)");

        // Таблица рецептов (без отдельной таблицы оценок)
        db.execSQL("CREATE TABLE " + TABLE_RECIPES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, description TEXT, ingredients TEXT, instructions TEXT, " +
                "image BLOB, author_id INTEGER, rating REAL DEFAULT 0, rating_count INTEGER DEFAULT 0)");

        // Таблица отзывов (теперь основная для оценок)
        db.execSQL("CREATE TABLE " + TABLE_REVIEWS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "recipe_id INTEGER, user_id INTEGER, username TEXT, " +
                "review_text TEXT, rating INTEGER, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE(recipe_id, user_id), " +
                "FOREIGN KEY(recipe_id) REFERENCES " + TABLE_RECIPES + "(id))");

        // Админ по умолчанию
        ContentValues admin = new ContentValues();
        admin.put("username", "admin");
        admin.put("password", "admin123");
        admin.put("is_admin", 1);
        db.insert(TABLE_USERS, null, admin);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REVIEWS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result != -1;
    }

    public User loginUser(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{"id", "username", "is_admin"},
                "username=? AND password=?", new String[]{username, password}, null, null, null);

        if (cursor.moveToFirst()) {
            User user = new User();
            user.setId(cursor.getInt(0));
            user.setUsername(cursor.getString(1));
            user.setAdmin(cursor.getInt(2) == 1);
            cursor.close();
            db.close();
            return user;
        }
        cursor.close();
        db.close();
        return null;
    }

    public boolean addRecipe(Recipe recipe) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", recipe.getTitle());
        values.put("description", recipe.getDescription());
        values.put("ingredients", recipe.getIngredients());
        values.put("instructions", recipe.getInstructions());
        values.put("author_id", recipe.getAuthorId());

        if (recipe.getImage() != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            recipe.getImage().compress(Bitmap.CompressFormat.PNG, 100, stream);
            values.put("image", stream.toByteArray());
        }

        long result = db.insert(TABLE_RECIPES, null, values);
        db.close();
        return result != -1;
    }

    public boolean updateRecipe(Recipe recipe) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", recipe.getTitle());
        values.put("description", recipe.getDescription());
        values.put("ingredients", recipe.getIngredients());
        values.put("instructions", recipe.getInstructions());

        if (recipe.getImage() != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            recipe.getImage().compress(Bitmap.CompressFormat.PNG, 100, stream);
            values.put("image", stream.toByteArray());
        }

        int result = db.update(TABLE_RECIPES, values, "id=?", new String[]{String.valueOf(recipe.getId())});
        db.close();
        return result > 0;
    }

    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT r.*, u.username FROM " + TABLE_RECIPES + " r " +
                "LEFT JOIN " + TABLE_USERS + " u ON r.author_id = u.id ORDER BY r.id DESC", null);

        while (cursor.moveToNext()) {
            Recipe recipe = new Recipe();
            recipe.setId(cursor.getInt(0));
            recipe.setTitle(cursor.getString(1));
            recipe.setDescription(cursor.getString(2));
            recipe.setIngredients(cursor.getString(3));
            recipe.setInstructions(cursor.getString(4));
            recipe.setAuthorId(cursor.getInt(6));
            recipe.setRating(cursor.getFloat(7));
            recipe.setRatingCount(cursor.getInt(8));
            recipe.setAuthorName(cursor.getString(9));

            byte[] imageBytes = cursor.getBlob(5);
            if (imageBytes != null) {
                recipe.setImage(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
            }
            recipes.add(recipe);
        }
        cursor.close();
        db.close();
        return recipes;
    }

    public Recipe getRecipeById(int recipeId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT r.*, u.username FROM " + TABLE_RECIPES + " r " +
                "LEFT JOIN " + TABLE_USERS + " u ON r.author_id = u.id WHERE r.id=?", new String[]{String.valueOf(recipeId)});

        if (cursor.moveToFirst()) {
            Recipe recipe = new Recipe();
            recipe.setId(cursor.getInt(0));
            recipe.setTitle(cursor.getString(1));
            recipe.setDescription(cursor.getString(2));
            recipe.setIngredients(cursor.getString(3));
            recipe.setInstructions(cursor.getString(4));
            recipe.setAuthorId(cursor.getInt(6));
            recipe.setRating(cursor.getFloat(7));
            recipe.setRatingCount(cursor.getInt(8));
            recipe.setAuthorName(cursor.getString(9));

            byte[] imageBytes = cursor.getBlob(5);
            if (imageBytes != null) {
                recipe.setImage(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
            }
            cursor.close();
            db.close();
            return recipe;
        }
        cursor.close();
        db.close();
        return null;
    }

    public List<Recipe> searchRecipes(String query) {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String searchPattern = "%" + query + "%";
        Cursor cursor = db.rawQuery("SELECT r.*, u.username FROM " + TABLE_RECIPES + " r " +
                        "LEFT JOIN " + TABLE_USERS + " u ON r.author_id = u.id " +
                        "WHERE r.title LIKE ? OR r.description LIKE ? OR r.ingredients LIKE ?",
                new String[]{searchPattern, searchPattern, searchPattern});

        while (cursor.moveToNext()) {
            Recipe recipe = new Recipe();
            recipe.setId(cursor.getInt(0));
            recipe.setTitle(cursor.getString(1));
            recipe.setDescription(cursor.getString(2));
            recipe.setIngredients(cursor.getString(3));
            recipe.setInstructions(cursor.getString(4));
            recipe.setAuthorId(cursor.getInt(6));
            recipe.setRating(cursor.getFloat(7));
            recipe.setRatingCount(cursor.getInt(8));
            recipe.setAuthorName(cursor.getString(9));

            byte[] imageBytes = cursor.getBlob(5);
            if (imageBytes != null) {
                recipe.setImage(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
            }
            recipes.add(recipe);
        }
        cursor.close();
        db.close();
        return recipes;
    }

    // Добавление отзыва и обновление рейтинга рецепта
    public boolean addReview(int recipeId, int userId, String username, String reviewText, int rating) {
        SQLiteDatabase db = getWritableDatabase();

        Cursor authorCursor = db.query(TABLE_RECIPES, new String[]{"author_id"},
                "id=?", new String[]{String.valueOf(recipeId)}, null, null, null);
        if (authorCursor.moveToFirst()) {
            int authorId = authorCursor.getInt(0);
            if (authorId == userId) {
                authorCursor.close();
                db.close();
                return false;
            }
        }
        authorCursor.close();

        // Проверка на повторный отзыв
        Cursor checkCursor = db.query(TABLE_REVIEWS, new String[]{"id"},
                "recipe_id=? AND user_id=?", new String[]{String.valueOf(recipeId), String.valueOf(userId)},
                null, null, null);

        if (checkCursor.moveToFirst()) {
            checkCursor.close();
            db.close();
            return false;
        }
        checkCursor.close();

        // Добавляем отзыв
        ContentValues values = new ContentValues();
        values.put("recipe_id", recipeId);
        values.put("user_id", userId);
        values.put("username", username);
        values.put("review_text", reviewText);
        values.put("rating", rating);

        long result = db.insert(TABLE_REVIEWS, null, values);

        if (result != -1) {
            updateRecipeRating(recipeId);
        }

        db.close();
        return result != -1;
    }

    // Обновление рейтинга рецепта на основе всех отзывов
    private void updateRecipeRating(int recipeId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor avgCursor = db.rawQuery("SELECT AVG(rating), COUNT(*) FROM " + TABLE_REVIEWS +
                " WHERE recipe_id=?", new String[]{String.valueOf(recipeId)});

        if (avgCursor.moveToFirst()) {
            float newRating = (float) avgCursor.getDouble(0);
            int ratingCount = avgCursor.getInt(1);

            ContentValues recipeValues = new ContentValues();
            recipeValues.put("rating", newRating);
            recipeValues.put("rating_count", ratingCount);
            db.update(TABLE_RECIPES, recipeValues, "id=?", new String[]{String.valueOf(recipeId)});
        }
        avgCursor.close();
        db.close();
    }

    // Получение всех отзывов для рецепта
    public List<Review> getReviewsForRecipe(int recipeId) {
        List<Review> reviews = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_REVIEWS +
                " WHERE recipe_id=? ORDER BY created_at DESC", new String[]{String.valueOf(recipeId)});

        while (cursor.moveToNext()) {
            Review review = new Review();
            review.setId(cursor.getInt(0));
            review.setRecipeId(cursor.getInt(1));
            review.setUserId(cursor.getInt(2));
            review.setUsername(cursor.getString(3));
            review.setReviewText(cursor.getString(4));
            review.setRating(cursor.getInt(5));
            review.setCreatedAt(cursor.getString(6));
            reviews.add(review);
        }
        cursor.close();
        db.close();
        return reviews;
    }

    public boolean deleteRecipe(int recipeId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_REVIEWS, "recipe_id=?", new String[]{String.valueOf(recipeId)});
        int result = db.delete(TABLE_RECIPES, "id=?", new String[]{String.valueOf(recipeId)});
        db.close();
        return result > 0;
    }

    public List<Recipe> getUserRecipes(int userId) {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT r.*, u.username FROM " + TABLE_RECIPES + " r " +
                        "LEFT JOIN " + TABLE_USERS + " u ON r.author_id = u.id WHERE r.author_id=? ORDER BY r.id DESC",
                new String[]{String.valueOf(userId)});

        while (cursor.moveToNext()) {
            Recipe recipe = new Recipe();
            recipe.setId(cursor.getInt(0));
            recipe.setTitle(cursor.getString(1));
            recipe.setDescription(cursor.getString(2));
            recipe.setIngredients(cursor.getString(3));
            recipe.setInstructions(cursor.getString(4));
            recipe.setAuthorId(cursor.getInt(6));
            recipe.setRating(cursor.getFloat(7));
            recipe.setRatingCount(cursor.getInt(8));
            recipe.setAuthorName(cursor.getString(9));

            byte[] imageBytes = cursor.getBlob(5);
            if (imageBytes != null) {
                recipe.setImage(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
            }
            recipes.add(recipe);
        }
        cursor.close();
        db.close();
        return recipes;
    }

    public boolean hasUserReviewed(int recipeId, int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_REVIEWS, new String[]{"id"},
                "recipe_id=? AND user_id=?", new String[]{String.valueOf(recipeId), String.valueOf(userId)},
                null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    public boolean isRecipeAuthor(int recipeId, int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RECIPES, new String[]{"id"},
                "id=? AND author_id=?", new String[]{String.valueOf(recipeId), String.valueOf(userId)},
                null, null, null);
        boolean isAuthor = cursor.moveToFirst();
        cursor.close();
        db.close();
        return isAuthor;
    }

    public boolean deleteReview(int reviewId, int recipeId) {
        SQLiteDatabase db = getWritableDatabase();
        int result = db.delete(TABLE_REVIEWS, "id=?", new String[]{String.valueOf(reviewId)});

        if (result > 0) {
            // Обновляем рейтинг рецепта после удаления отзыва
            updateRecipeRating(recipeId);
        }

        db.close();
        return result > 0;
    }


}