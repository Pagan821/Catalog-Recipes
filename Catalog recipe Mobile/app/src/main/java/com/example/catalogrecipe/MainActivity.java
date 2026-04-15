package com.example.catalogrecipe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin, btnRegister;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> login());
        btnRegister.setOnClickListener(v -> showRegisterDialog());
    }

    private void login() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = dbHelper.loginUser(username, password);

        if (user != null) {
            Toast.makeText(this, "Добро пожаловать, " + username, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, RecipeActivity.class);
            intent.putExtra("user_id", user.getId());
            intent.putExtra("username", user.getUsername());
            intent.putExtra("is_admin", user.isAdmin());
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Неверное имя или пароль", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRegisterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Регистрация");

        View view = getLayoutInflater().inflate(R.layout.dialog_register, null);
        EditText etNewUsername = view.findViewById(R.id.etNewUsername);
        EditText etNewPassword = view.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = view.findViewById(R.id.etConfirmPassword);

        builder.setView(view);
        builder.setPositiveButton("Зарегистрироваться", (dialog, which) -> {
            String username = etNewUsername.getText().toString().trim();
            String password = etNewPassword.getText().toString().trim();
            String confirm = etConfirmPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(confirm)) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 4) {
                Toast.makeText(this, "Пароль минимум 4 символа", Toast.LENGTH_SHORT).show();
            } else if (dbHelper.registerUser(username, password)) {
                Toast.makeText(this, "Регистрация успешна! Теперь войдите", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Пользователь уже существует", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
}