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
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_RECIPES = "recipes";
    private static final String TABLE_RATINGS = "ratings";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE, password TEXT, is_admin INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, description TEXT, ingredients TEXT, instructions TEXT, " +
                "image BLOB, author_id INTEGER, rating REAL DEFAULT 0, rating_count INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_RATINGS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "recipe_id INTEGER, user_id INTEGER, rating_value INTEGER, " +
                "UNIQUE(recipe_id, user_id))");

        // Админ по умолчанию
        ContentValues admin = new ContentValues();
        admin.put("username", "admin");
        admin.put("password", "admin123");
        admin.put("is_admin", 1);
        db.insert(TABLE_USERS, null, admin);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RATINGS);
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

    public boolean rateRecipe(int recipeId, int userId, int rating) {
        SQLiteDatabase db = getWritableDatabase();

        // Проверка
        Cursor cursor = db.query(TABLE_RATINGS, new String[]{"id"},
                "recipe_id=? AND user_id=?", new String[]{String.valueOf(recipeId), String.valueOf(userId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            cursor.close();
            db.close();
            return false;
        }
        cursor.close();

        // Добавляем оценку
        ContentValues ratingValues = new ContentValues();
        ratingValues.put("recipe_id", recipeId);
        ratingValues.put("user_id", userId);
        ratingValues.put("rating_value", rating);
        db.insert(TABLE_RATINGS, null, ratingValues);

        // Обновляем средний рейтинг
        Cursor avgCursor = db.rawQuery("SELECT AVG(rating_value), COUNT(*) FROM " + TABLE_RATINGS +
                " WHERE recipe_id=?", new String[]{String.valueOf(recipeId)});

        if (avgCursor.moveToFirst()) {
            ContentValues recipeValues = new ContentValues();
            recipeValues.put("rating", avgCursor.getDouble(0));
            recipeValues.put("rating_count", avgCursor.getInt(1));
            db.update(TABLE_RECIPES, recipeValues, "id=?", new String[]{String.valueOf(recipeId)});
        }
        avgCursor.close();
        db.close();
        return true;
    }

    public boolean deleteRecipe(int recipeId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_RATINGS, "recipe_id=?", new String[]{String.valueOf(recipeId)});
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

    public boolean hasUserRatedRecipe(int recipeId, int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RATINGS, new String[]{"id"},
                "recipe_id=? AND user_id=?", new String[]{String.valueOf(recipeId), String.valueOf(userId)},
                null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }
}