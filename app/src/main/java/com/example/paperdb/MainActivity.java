package com.example.paperdb;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.paperdb.Paper;

public class MainActivity extends AppCompatActivity {
    EditText nameText, descriptionText, priceText;
    Button addBtn, delBtn, updBtn, chooseImageBtn;
    ListView listView;
    ImageView clothesImageView;
    ArrayAdapter<String> adapter;
    String selectedClothesName;
    Bitmap selectedImage;

    private static final int PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация PaperDb
        Paper.init(this);

        nameText = findViewById(R.id.nameText);
        descriptionText = findViewById(R.id.descriptionText);
        priceText = findViewById(R.id.priceText);
        addBtn = findViewById(R.id.addButton);
        updBtn = findViewById(R.id.updateButton);
        delBtn = findViewById(R.id.deleteButton);
        chooseImageBtn = findViewById(R.id.chooseImageButton);
        clothesImageView = findViewById(R.id.clothesImageView);
        listView = findViewById(R.id.listView);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, getClothesNames());
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, i, l) -> {
            selectedClothesName = adapter.getItem(i);
            assert selectedClothesName != null;
            Clothes clothes = Paper.book().read(selectedClothesName, null);
            if (clothes != null) {
                nameText.setText(clothes.getName());
                descriptionText.setText(clothes.getDescription());
                priceText.setText(String.valueOf(clothes.getPrice()));
                // Загрузка изображения по пути
                selectedImage = BitmapFactory.decodeFile(clothes.getImagePath());
                clothesImageView.setImageBitmap(selectedImage);
            }
        });

        chooseImageBtn.setOnClickListener(v -> {
            Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickPhoto, PICK_IMAGE);
        });

        addBtn.setOnClickListener(v -> {
            String name = nameText.getText().toString();
            String description = descriptionText.getText().toString();
            String priceString = priceText.getText().toString();

            if (!name.isEmpty() && !description.isEmpty() && !priceString.isEmpty() && selectedImage != null) {
                double price = Double.parseDouble(priceString);
                String imagePath = saveImageToFile(selectedImage).getPath();
                Clothes clothes = new Clothes(name, description, price, imagePath);
                Paper.book().write(name, clothes);
                updateClothesList();
                clearInputs();
            }
        });

        updBtn.setOnClickListener(v -> {
            if (selectedClothesName == null) {
                Toast.makeText(MainActivity.this, "Выберите товар.", Toast.LENGTH_SHORT).show();
                return;
            }

            String name = nameText.getText().toString();
            String description = descriptionText.getText().toString();
            String priceString = priceText.getText().toString();

            if (!name.isEmpty() && !description.isEmpty() && !priceString.isEmpty() && selectedImage != null) {
                double price = Double.parseDouble(priceString);
                String imagePath = saveImageToFile(selectedImage).getPath();
                Clothes updatedClothes = new Clothes(name, description, price, imagePath);
                Paper.book().delete(selectedClothesName);
                Paper.book().write(name, updatedClothes);
                updateClothesList();
                clearInputs();
            }
        });

        delBtn.setOnClickListener(v -> {
            if (selectedClothesName == null) {
                Toast.makeText(MainActivity.this, "Выберите товар.", Toast.LENGTH_SHORT).show();
                return;
            }

            Paper.book().delete(selectedClothesName);
            updateClothesList();
            clearInputs();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            try {
                assert selectedImageUri != null;
                InputStream imageStream = getContentResolver().openInputStream(selectedImageUri);
                selectedImage = BitmapFactory.decodeStream(imageStream);
                clothesImageView.setImageBitmap(selectedImage);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Uri saveImageToFile(Bitmap bitmap) {
        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "clothes_images");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File imageFile = new File(directory, System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            return Uri.fromFile(imageFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void clearInputs() {
        nameText.setText("");
        descriptionText.setText("");
        priceText.setText("");
        selectedImage = null;
        clothesImageView.setImageBitmap(null);
        selectedClothesName = null;
    }

    private void updateClothesList() {
        adapter.clear();
        adapter.addAll(getClothesNames());
        adapter.notifyDataSetChanged();
    }

    private List<String> getClothesNames() {
        return new ArrayList<>(Paper.book().getAllKeys());
    }
}
