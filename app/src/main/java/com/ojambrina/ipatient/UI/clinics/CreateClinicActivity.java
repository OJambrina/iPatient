package com.ojambrina.ipatient.UI.clinics;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ojambrina.ipatient.BuildConfig;
import com.ojambrina.ipatient.R;
import com.ojambrina.ipatient.UI.home.HomeActivity;
import com.ojambrina.ipatient.entities.Clinic;
import com.ojambrina.ipatient.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.ojambrina.ipatient.utils.Constants.CLINIC;
import static com.ojambrina.ipatient.utils.Constants.CLINICS;
import static com.ojambrina.ipatient.utils.Constants.CLINIC_LIST;
import static com.ojambrina.ipatient.utils.Constants.CLINIC_NAME;
import static com.ojambrina.ipatient.utils.Constants.LATEST_CLINIC;
import static com.ojambrina.ipatient.utils.Constants.SHARED_PREFERENCES;
import static com.ojambrina.ipatient.utils.RequestCodes.IMAGE_FROM_CAMERA;
import static com.ojambrina.ipatient.utils.RequestCodes.IMAGE_FROM_GALLERY;

public class CreateClinicActivity extends AppCompatActivity {

    //Butterknife
    @BindView(R.id.edit_clinic_name)
    EditText editClinicName;
    @BindView(R.id.edit_clinic_password)
    EditText editClinicPassword;
    @BindView(R.id.edit_clinic_direction)
    EditText editClinicDirection;
    @BindView(R.id.edit_clinic_identity_number)
    EditText editClinicIdentityNumber;
    @BindView(R.id.edit_clinic_description)
    EditText editClinicDescription;
    @BindView(R.id.button_clinic_register)
    Button buttonClinicRegister;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.image_clinic)
    CircleImageView imageClinic;
    @BindView(R.id.image_camera)
    ImageView imageCamera;
    @BindView(R.id.delete_background_photo)
    ImageView deleteBackgroundPhoto;
    @BindView(R.id.layout_background_delete_photo)
    LinearLayout layoutBackgroundDeletePhoto;
    @BindView(R.id.layout_profile_photo)
    RelativeLayout layoutProfilePhoto;

    //Declarations
    Context context;
    Clinic clinic;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    FirebaseFirestore firebaseFirestore;
    String name, password, direction, clinicIdentityNumber, description;
    boolean isValidClinicName, isValidClinicPassword;
    SharedPreferences sharedPreferences;
    List<Clinic> clinicList = new ArrayList<>();
    private String cameraPath;
    private File profilePath;
    private Uri imageUri;
    private Uri getImageUri;

    //TODO METODO PARA AÑADIR FOTO A LA CLÍNICA

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_clinic);
        ButterKnife.bind(this);

        databaseReference = FirebaseDatabase.getInstance().getReference();
        context = this;
        sharedPreferences = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);

        setToolbar();
        setFirebase();
        listeners();
    }

    private void listeners() {
        buttonClinicRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getStrings();

                validateClinicPassword(editClinicPassword);
                validateClinicName(editClinicName);

                if (isValidClinicName && isValidClinicPassword) {
                    firebaseFirestore.collection(CLINICS).document(name).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.getResult().exists()) {
                                Toast.makeText(context, "Ya existe una clínica con ese nombre", Toast.LENGTH_SHORT).show();
                            } else {
                                addClinic();
                                if (sharedPreferences.getString(CLINIC_LIST, "").isEmpty()) {
                                    clinicList.clear();
                                    Gson gson = new Gson();
                                    clinicList.add(clinic);
                                    String listOfClinics = gson.toJson(clinicList);

                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString(CLINIC_LIST, listOfClinics);
                                    editor.putString(LATEST_CLINIC, clinic.getName());
                                    editor.apply();
                                } else {
                                    clinicList.clear();
                                    Gson gson = new Gson();
                                    String json = sharedPreferences.getString(CLINIC_LIST, "");

                                    Type type = new TypeToken<List<Clinic>>() {
                                    }.getType();
                                    List<Clinic> clinicList = gson.fromJson(json, type);

                                    clinicList.add(clinic);
                                    String listOfClinics = gson.toJson(clinicList);

                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString(CLINIC_LIST, listOfClinics);
                                    editor.putString(LATEST_CLINIC, clinic.getName());
                                    editor.apply();
                                }

                                firebaseFirestore.collection(CLINICS).document(name).set(clinic);
                                Toast.makeText(context, "Clínica agregada correctamente", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(context, HomeActivity.class);
                                intent.putExtra(CLINIC_NAME, clinic.getName());
                                intent.putExtra(CLINIC, clinic);
                                startActivity(intent);
                                finish();
                            }
                        }
                    });
                }
            }
        });

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void getStrings() {
        name = editClinicName.getText().toString().trim();
        password = editClinicPassword.getText().toString().trim();
        direction = editClinicDescription.getText().toString().trim();
        clinicIdentityNumber = editClinicIdentityNumber.getText().toString().trim();
        description = editClinicDescription.getText().toString().trim();
    }

    private void addClinic() {
        clinic = new Clinic();

        clinic.setName(name);
        clinic.setPassword(password);
        clinic.setDirection(direction);
        clinic.setIdentityNumber(clinicIdentityNumber);
        clinic.setDescription(description);
    }

    private void setFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("clinicas");
        firebaseFirestore = FirebaseFirestore.getInstance();
    }

    private void showPictureDialog() {
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle("Selecciona una opción");
        String[] pictureDialogItems = {
                "Seleccionar foto desde galería",
                "Capturar foto desde camara"};
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                choosePhotoFromGallery();
                                break;
                            case 1:
                                takePhotoFromCamera();
                                break;
                        }
                    }
                });
        pictureDialog.show();
    }

    private void choosePhotoFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, IMAGE_FROM_GALLERY);
    }

    private void takePhotoFromCamera() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, IMAGE_FROM_CAMERA);
        } else {
            cameraPath = openCamera(context, IMAGE_FROM_CAMERA);
        }
    }

    public String openCamera(Context context, int backgroundOrProfile) {
        String pictureImagePath;
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String imageFileName = timeStamp + ".jpg";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        pictureImagePath = storageDir.getAbsolutePath() + "/" + imageFileName;

        File file = new File(pictureImagePath);
        Uri outpuUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outpuUri);
        startActivityForResult(cameraIntent, backgroundOrProfile);

        return pictureImagePath;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == -1) {
            switch (requestCode) {
                case IMAGE_FROM_GALLERY:
                    if (data != null) {
                        imageUri = data.getData();
                        Bitmap bitmap = null;
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), data.getData());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Glide.with(context)
                                .asBitmap()
                                .load(bitmap)
                                .into(imageClinic);
                        imageCamera.setVisibility(View.GONE);
                        layoutBackgroundDeletePhoto.setVisibility(View.VISIBLE);
                    }
                    break;
                case IMAGE_FROM_CAMERA:
                    profilePath = new File(cameraPath);
                    imageUri = Uri.fromFile(profilePath);
                    Glide.with(context)
                            .load(profilePath)
                            .into(imageClinic);
                    imageCamera.setVisibility(View.GONE);
                    layoutBackgroundDeletePhoto.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_LONG).show();
            cameraPath = openCamera(context, IMAGE_FROM_CAMERA);
        } else {
            Toast.makeText(this, "Permisos denegados", Toast.LENGTH_LONG).show();
            Utils.permissionDialog(context);
        }
    }

    private String getExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    //VALIDATIONS
    private void validateClinicName(EditText editClinicName) {
        name = editClinicName.getText().toString().trim();
        if (name.length() > 0) {
            isValidClinicName = true;
        } else {
            editClinicName.requestFocus();
            editClinicName.setError("El campo nombre no puede estar vacío");
            isValidClinicName = false;
        }
    }

    private void validateClinicPassword(EditText editClinicPassword) {
        password = editClinicPassword.getText().toString().trim();
        if (password.length() >= 8) {
            isValidClinicPassword = true;
        } else {
            editClinicPassword.requestFocus();
            editClinicPassword.setError("Al menos 8 caracteres");
            isValidClinicPassword = false;
        }
    }
}
