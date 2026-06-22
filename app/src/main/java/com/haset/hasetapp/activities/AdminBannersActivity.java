package com.haset.hasetapp.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.ValueEventListener;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.PatientBannerAdapter;
import com.haset.hasetapp.utils.Constants;

import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.AdminBannersViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminBannersActivity extends AppCompatActivity {

    private RecyclerView rvBanners;
    private MaterialButton btnAddBanner;
    private ImageView btnBack;
    private AdminBannerAdapter adapter;
    private List<PatientBannerAdapter.BannerItem> bannerList;
    private AdminBannersViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_banners);

        initViews();
        viewModel = new ViewModelProvider(this).get(AdminBannersViewModel.class);
        setupObservers();
    }

    private void setupObservers() {
        viewModel.getBanners().observe(this, banners -> {
            if (banners != null) {
                bannerList.clear();
                bannerList.addAll(banners);
                
                if (bannerList.isEmpty()) {
                    addDefaultBanners();
                    addDefaultBannersToFirebase();
                }
                
                adapter.notifyDataSetChanged();
            } else {
                bannerList.clear();
                addDefaultBanners();
                addDefaultBannersToFirebase();
                adapter.notifyDataSetChanged();
            }
        });
        
        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void addDefaultBanners() {
        PatientBannerAdapter.BannerItem pharmacy1 = new PatientBannerAdapter.BannerItem();
        pharmacy1.key = "default_pharmacy_1";
        pharmacy1.titleLine1 = "Up to";
        pharmacy1.titleLine2 = "50% OFF";
        pharmacy1.discount = "Flash Sale";
        pharmacy1.buttonText = "Shop Now";
        pharmacy1.bannerType = PatientBannerAdapter.BannerItem.BannerType.PHARMACY;
        bannerList.add(pharmacy1);
        
        PatientBannerAdapter.BannerItem messaging1 = new PatientBannerAdapter.BannerItem();
        messaging1.key = "default_messaging_1";
        messaging1.titleLine1 = "Online";
        messaging1.titleLine2 = "Consultation";
        messaging1.discount = "Live Now";
        messaging1.buttonText = "Chat Now";
        messaging1.bannerType = PatientBannerAdapter.BannerItem.BannerType.MESSAGING;
        bannerList.add(messaging1);
        
        PatientBannerAdapter.BannerItem appointment1 = new PatientBannerAdapter.BannerItem();
        appointment1.key = "default_appointment_1";
        appointment1.titleLine1 = "Book Expert";
        appointment1.titleLine2 = "Care Today";
        appointment1.discount = "Verified";
        appointment1.buttonText = "Book Now";
        appointment1.bannerType = PatientBannerAdapter.BannerItem.BannerType.APPOINTMENT;
        bannerList.add(appointment1);
        
        PatientBannerAdapter.BannerItem pharmacy2 = new PatientBannerAdapter.BannerItem();
        pharmacy2.key = "default_pharmacy_2";
        pharmacy2.titleLine1 = "Premium";
        pharmacy2.titleLine2 = "Home Care";
        pharmacy2.discount = "30% OFF";
        pharmacy2.buttonText = "Explore";
        pharmacy2.bannerType = PatientBannerAdapter.BannerItem.BannerType.PHARMACY;
        bannerList.add(pharmacy2);
    }
    
    private void addDefaultBannersToFirebase() {
        PatientBannerAdapter.BannerItem pharmacy1 = new PatientBannerAdapter.BannerItem();
        pharmacy1.titleLine1 = "Up to";
        pharmacy1.titleLine2 = "50% OFF";
        pharmacy1.discount = "Flash Sale";
        pharmacy1.buttonText = "Shop Now";
        pharmacy1.bannerType = PatientBannerAdapter.BannerItem.BannerType.PHARMACY;
        
        PatientBannerAdapter.BannerItem messaging1 = new PatientBannerAdapter.BannerItem();
        messaging1.titleLine1 = "Online";
        messaging1.titleLine2 = "Consultation";
        messaging1.discount = "Live Now";
        messaging1.buttonText = "Chat Now";
        messaging1.bannerType = PatientBannerAdapter.BannerItem.BannerType.MESSAGING;
        
        PatientBannerAdapter.BannerItem appointment1 = new PatientBannerAdapter.BannerItem();
        appointment1.titleLine1 = "Book Expert";
        appointment1.titleLine2 = "Care Today";
        appointment1.discount = "Verified";
        appointment1.buttonText = "Book Now";
        appointment1.bannerType = PatientBannerAdapter.BannerItem.BannerType.APPOINTMENT;
        
        PatientBannerAdapter.BannerItem pharmacy2 = new PatientBannerAdapter.BannerItem();
        pharmacy2.titleLine1 = "Premium";
        pharmacy2.titleLine2 = "Home Care";
        pharmacy2.discount = "30% OFF";
        pharmacy2.buttonText = "Explore";
        pharmacy2.bannerType = PatientBannerAdapter.BannerItem.BannerType.PHARMACY;
        
        viewModel.addBanner(pharmacy1);
        viewModel.addBanner(messaging1);
        viewModel.addBanner(appointment1);
        viewModel.addBanner(pharmacy2);
    }

    private void initViews() {
        rvBanners = findViewById(R.id.rvBanners);
        btnAddBanner = findViewById(R.id.btnAddBanner);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnAddBanner.setOnClickListener(v -> showBannerSheet(null));

        bannerList = new ArrayList<>();
        adapter = new AdminBannerAdapter(bannerList, new AdminBannerAdapter.OnActionListener() {
            @Override
            public void onDelete(int position) {
                deleteBanner(position);
            }

            @Override
            public void onEdit(PatientBannerAdapter.BannerItem banner) {
                showBannerSheet(banner);
            }
        });
        rvBanners.setLayoutManager(new LinearLayoutManager(this));
        rvBanners.setAdapter(adapter);
    }


    private void deleteBanner(int position) {
        PatientBannerAdapter.BannerItem item = bannerList.get(position);
        if (item.key != null) {
            viewModel.deleteBanner(item.key);
        }
    }

    private void showBannerSheet(@Nullable PatientBannerAdapter.BannerItem banner) {
        CreateBannerBottomSheet sheet = CreateBannerBottomSheet.newInstance(banner);
        sheet.show(getSupportFragmentManager(), "BannerSheet");
    }

    // --- Adapter ---
    private static class AdminBannerAdapter extends RecyclerView.Adapter<AdminBannerAdapter.ViewHolder> {
        private final List<PatientBannerAdapter.BannerItem> items;
        private final OnActionListener actionListener;

        public interface OnActionListener {
            void onDelete(int position);
            void onEdit(PatientBannerAdapter.BannerItem banner);
        }

        AdminBannerAdapter(List<PatientBannerAdapter.BannerItem> items, OnActionListener actionListener) {
            this.items = items;
            this.actionListener = actionListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner_list, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PatientBannerAdapter.BannerItem item = items.get(position);
            
            String titleText = (item.titleLine1 != null ? item.titleLine1 : "") + " " + (item.titleLine2 != null ? item.titleLine2 : "");
            holder.tvTitle.setText(titleText.trim());
            holder.tvType.setText("Type: " + (item.bannerType != null ? item.bannerType.name() : "N/A"));
            holder.tvBadge.setText("Badge: " + (item.discount != null ? item.discount : ""));

            // Check if default banner
            boolean isDefault = item.key != null && item.key.startsWith("default_");
            holder.tvDefaultBadge.setVisibility(isDefault ? View.VISIBLE : View.GONE);

            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext()).load(item.imageUrl).into(holder.ivPreview);
            } else if (item.imageRes != 0) {
                holder.ivPreview.setImageResource(item.imageRes);
            } else {
                holder.ivPreview.setImageResource(R.drawable.placeholder_image);
            }

            holder.btnDelete.setOnClickListener(v -> actionListener.onDelete(position));
            holder.btnEdit.setOnClickListener(v -> actionListener.onEdit(item));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPreview, btnDelete, btnEdit;
            TextView tvTitle, tvType, tvBadge, tvDefaultBadge;
            ViewHolder(View v) {
                super(v);
                ivPreview = v.findViewById(R.id.ivBannerPreview);
                btnDelete = v.findViewById(R.id.btnDeleteBanner);
                btnEdit = v.findViewById(R.id.btnEditBanner);
                tvTitle = v.findViewById(R.id.tvAdminTitle);
                tvType = v.findViewById(R.id.tvAdminType);
                tvBadge = v.findViewById(R.id.tvAdminBadge);
                tvDefaultBadge = v.findViewById(R.id.tvDefaultBadge);
            }
        }
    }

    // --- Bottom Sheet ---
    public static class CreateBannerBottomSheet extends BottomSheetDialogFragment {
        private static final int PICK_IMAGE_REQUEST = 1;
        private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
        private static final String[] ALLOWED_FORMATS = {"jpg", "jpeg", "png", "webp"};

        private Uri selectedImageUri;
        private ImageView ivPreview, ivPreviewDetailed;
        private PatientBannerAdapter.BannerItem existingBanner;
        private boolean isDetailedBanner = true;
        private AdminBannersViewModel viewModel;

        public static CreateBannerBottomSheet newInstance(@Nullable PatientBannerAdapter.BannerItem banner) {
            CreateBannerBottomSheet fragment = new CreateBannerBottomSheet();
            if (banner != null) {
                Bundle args = new Bundle();
                args.putSerializable("banner", banner);
                fragment.setArguments(args);
            }
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                existingBanner = (PatientBannerAdapter.BannerItem) getArguments().getSerializable("banner");
                if (existingBanner != null) {
                    isDetailedBanner = existingBanner.bannerType != PatientBannerAdapter.BannerItem.BannerType.IMAGE_BANNER;
                }
            }
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.bottom_sheet_create_banner, container, false);
            viewModel = new ViewModelProvider(requireActivity()).get(AdminBannersViewModel.class);

            LinearLayout layoutTypeSelection = v.findViewById(R.id.layoutBannerTypeSelection);
            LinearLayout layoutDetailedForm = v.findViewById(R.id.layoutDetailedForm);
            LinearLayout layoutImageForm = v.findViewById(R.id.layoutImageForm);
            RadioGroup rgBannerType = v.findViewById(R.id.rgBannerType);
            RadioButton rbDetailed = v.findViewById(R.id.rbDetailedBanner);
            RadioButton rbImage = v.findViewById(R.id.rbImageBanner);
            MaterialButton btnNext = v.findViewById(R.id.btnNextStep);
            MaterialButton btnBackDetailed = v.findViewById(R.id.btnBackDetailed);
            MaterialButton btnSubmitDetailed = v.findViewById(R.id.btnSubmitDetailed);
            MaterialButton btnBackImage = v.findViewById(R.id.btnBackImage);
            MaterialButton btnSubmitImage = v.findViewById(R.id.btnSubmitImage);
            ivPreview = v.findViewById(R.id.ivPreview);
            ivPreviewDetailed = v.findViewById(R.id.ivPreviewDetailed);

            if (existingBanner != null) {
                v.findViewById(R.id.tvSheetTitle).setVisibility(View.GONE);
                if (existingBanner.bannerType == PatientBannerAdapter.BannerItem.BannerType.IMAGE_BANNER) {
                    rbImage.setChecked(true);
                    isDetailedBanner = false;
                    showImageForm(layoutTypeSelection, layoutDetailedForm, layoutImageForm);
                    populateImageForm(v);
                } else {
                    rbDetailed.setChecked(true);
                    isDetailedBanner = true;
                    showDetailedForm(layoutTypeSelection, layoutDetailedForm, layoutImageForm);
                    populateDetailedForm(v);
                }
            } else {
                layoutTypeSelection.setVisibility(View.VISIBLE);
                layoutDetailedForm.setVisibility(View.GONE);
                layoutImageForm.setVisibility(View.GONE);
            }

            btnNext.setOnClickListener(view -> {
                isDetailedBanner = rgBannerType.getCheckedRadioButtonId() == R.id.rbDetailedBanner;
                if (isDetailedBanner) {
                    showDetailedForm(layoutTypeSelection, layoutDetailedForm, layoutImageForm);
                } else {
                    showImageForm(layoutTypeSelection, layoutDetailedForm, layoutImageForm);
                }
            });

            btnBackDetailed.setOnClickListener(view -> showTypeSelection(layoutTypeSelection, layoutDetailedForm, layoutImageForm));
            btnBackImage.setOnClickListener(view -> showTypeSelection(layoutTypeSelection, layoutDetailedForm, layoutImageForm));

            MaterialButton btnSelectDetailed = v.findViewById(R.id.btnSelectImageDetailed);
            MaterialButton btnSelectImage = v.findViewById(R.id.btnSelectImage);

            btnSelectDetailed.setOnClickListener(view -> {
                selectedImageUri = null;
                openGallery();
            });

            btnSelectImage.setOnClickListener(view -> {
                selectedImageUri = null;
                openGallery();
            });

            btnSubmitDetailed.setOnClickListener(view -> {
                if (validateAndSubmitDetailed(v)) {
                    submitDetailedBanner(v);
                }
            });

            btnSubmitImage.setOnClickListener(view -> {
                if (validateAndSubmitImage(v)) {
                    submitImageBanner(v);
                }
            });

            viewModel.getProcessing().observe(getViewLifecycleOwner(), processing -> {
                if (processing != null && processing) {
                    com.haset.hasetapp.utils.CustomDialog.showLoading(getContext(), getString(R.string.loading));
                } else {
                    com.haset.hasetapp.utils.CustomDialog.hideLoading();
                }
            });

            return v;
        }

        private void showTypeSelection(LinearLayout typeSelection, LinearLayout detailedForm, LinearLayout imageForm) {
            typeSelection.setVisibility(View.VISIBLE);
            detailedForm.setVisibility(View.GONE);
            imageForm.setVisibility(View.GONE);
        }

        private void showDetailedForm(LinearLayout typeSelection, LinearLayout detailedForm, LinearLayout imageForm) {
            typeSelection.setVisibility(View.GONE);
            detailedForm.setVisibility(View.VISIBLE);
            imageForm.setVisibility(View.GONE);
        }

        private void showImageForm(LinearLayout typeSelection, LinearLayout detailedForm, LinearLayout imageForm) {
            typeSelection.setVisibility(View.GONE);
            detailedForm.setVisibility(View.GONE);
            imageForm.setVisibility(View.VISIBLE);
        }

        private void populateDetailedForm(View v) {
            TextInputEditText etTitle1 = v.findViewById(R.id.etTitle1);
            TextInputEditText etTitle2 = v.findViewById(R.id.etTitle2);
            TextInputEditText etBadge = v.findViewById(R.id.etBadge);
            TextInputEditText etButton = v.findViewById(R.id.etButton);
            RadioGroup rgActionType = v.findViewById(R.id.rgDetailedActionType);

            etTitle1.setText(existingBanner.titleLine1);
            etTitle2.setText(existingBanner.titleLine2);
            etBadge.setText(existingBanner.discount);
            etButton.setText(existingBanner.buttonText);

            setRadioFromType(existingBanner.bannerType, rgActionType);

            if (existingBanner.imageUrl != null && !existingBanner.imageUrl.isEmpty()) {
                ivPreviewDetailed.setVisibility(View.VISIBLE);
                Glide.with(this).load(existingBanner.imageUrl).into(ivPreviewDetailed);
            }
        }

        private void populateImageForm(View v) {
            if (existingBanner.imageUrl != null && !existingBanner.imageUrl.isEmpty()) {
                ivPreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(existingBanner.imageUrl).into(ivPreview);
                selectedImageUri = Uri.parse(existingBanner.imageUrl);
            }
        }

        private void setRadioFromType(PatientBannerAdapter.BannerItem.BannerType type, RadioGroup rgActionType) {
            if (type == null) return;
            int radioId = R.id.rbPharmacyDetailed;
            switch (type) {
                case MESSAGING: radioId = R.id.rbChatDetailed; break;
                case APPOINTMENT: radioId = R.id.rbDoctorsDetailed; break;
                case ARTICLE: radioId = R.id.rbArticleDetailed; break;
                default: radioId = R.id.rbPharmacyDetailed; break;
            }
            rgActionType.check(radioId);
        }

        private boolean validateAndSubmitDetailed(View v) {
            TextInputEditText etTitle1 = v.findViewById(R.id.etTitle1);
            TextInputEditText etTitle2 = v.findViewById(R.id.etTitle2);

            String t1 = etTitle1.getText().toString().trim();
            String t2 = etTitle2.getText().toString().trim();

            if (t1.isEmpty() || t2.isEmpty()) {
                Toast.makeText(getContext(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (existingBanner == null && selectedImageUri == null) {
                Toast.makeText(getContext(), R.string.select_an_image, Toast.LENGTH_SHORT).show();
                return false;
            }

            return selectedImageUri == null || validateImage(selectedImageUri);
        }

        private boolean validateAndSubmitImage(View v) {
            if (existingBanner == null && selectedImageUri == null) {
                Toast.makeText(getContext(), R.string.select_an_image, Toast.LENGTH_SHORT).show();
                return false;
            }

            return selectedImageUri == null || validateImage(selectedImageUri);
        }

        private boolean validateImage(Uri imageUri) {
            try {
                String mimeType = requireContext().getContentResolver().getType(imageUri);
                if (mimeType == null || !mimeType.startsWith("image/")) {
                    Toast.makeText(getContext(), R.string.invalid_image_format, Toast.LENGTH_SHORT).show();
                    return false;
                }

                String extension = mimeType.replace("image/", "").toLowerCase();
                boolean validFormat = false;
                for (String format : ALLOWED_FORMATS) {
                    if (format.equals(extension)) {
                        validFormat = true;
                        break;
                    }
                }
                if (!validFormat) {
                    Toast.makeText(getContext(), R.string.invalid_image_format, Toast.LENGTH_SHORT).show();
                    return false;
                }

                android.database.Cursor cursor = requireContext().getContentResolver().query(imageUri, null, null, null, null);
                if (cursor != null) {
                    int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    cursor.moveToFirst();
                    long size = cursor.getLong(sizeIndex);
                    cursor.close();

                    if (size > MAX_IMAGE_SIZE) {
                        Toast.makeText(getContext(), R.string.image_too_large, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), R.string.invalid_image_format, Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }

        private void submitDetailedBanner(View v) {
            TextInputEditText etTitle1 = v.findViewById(R.id.etTitle1);
            TextInputEditText etTitle2 = v.findViewById(R.id.etTitle2);
            TextInputEditText etBadge = v.findViewById(R.id.etBadge);
            TextInputEditText etButton = v.findViewById(R.id.etButton);
            RadioGroup rgActionType = v.findViewById(R.id.rgDetailedActionType);

            String t1 = etTitle1.getText().toString().trim();
            String t2 = etTitle2.getText().toString().trim();
            String b = etBadge.getText().toString().trim();
            String btnTxt = etButton.getText().toString().trim();

            PatientBannerAdapter.BannerItem.BannerType type = getTypeFromRadioGroup(rgActionType);

            if (selectedImageUri == null && existingBanner != null) {
                saveDetailedBanner(t1, t2, b, btnTxt, existingBanner.imageUrl, type);
            } else if (selectedImageUri != null) {
                viewModel.uploadBannerImage(requireContext(), selectedImageUri, t1, t2, b, btnTxt, type,
                    existingBanner != null ? existingBanner.key : null);
            }
        }

        private void submitImageBanner(View v) {
            if (selectedImageUri == null && existingBanner != null) {
                saveImageBanner(existingBanner.imageUrl, null);
            } else if (selectedImageUri != null) {
                viewModel.uploadBannerImage(requireContext(), selectedImageUri, null, null, null, null, 
                    PatientBannerAdapter.BannerItem.BannerType.IMAGE_BANNER, null,
                    existingBanner != null ? existingBanner.key : null);
            }
        }

        private PatientBannerAdapter.BannerItem.BannerType getTypeFromRadioGroup(RadioGroup rgActionType) {
            int checkedId = rgActionType.getCheckedRadioButtonId();
            if (checkedId == R.id.rbArticleDetailed) return PatientBannerAdapter.BannerItem.BannerType.ARTICLE;
            if (checkedId == R.id.rbDoctorsDetailed) return PatientBannerAdapter.BannerItem.BannerType.APPOINTMENT;
            if (checkedId == R.id.rbChatDetailed) return PatientBannerAdapter.BannerItem.BannerType.MESSAGING;
            return PatientBannerAdapter.BannerItem.BannerType.PHARMACY;
        }

        private void saveDetailedBanner(String t1, String t2, String b, String btnTxt, String imageUrl, PatientBannerAdapter.BannerItem.BannerType type) {
            PatientBannerAdapter.BannerItem item = new PatientBannerAdapter.BannerItem(t1, t2, b, btnTxt, imageUrl, type);
            if (existingBanner != null && existingBanner.key != null) {
                viewModel.updateBanner(existingBanner.key, item);
            } else {
                viewModel.addBanner(item);
            }
            observeSuccess();
        }

        private void saveImageBanner(String imageUrl, PatientBannerAdapter.BannerItem.BannerType actionType) {
            PatientBannerAdapter.BannerItem item = PatientBannerAdapter.BannerItem.createImageBanner(imageUrl, actionType);
            if (existingBanner != null && existingBanner.key != null) {
                viewModel.updateBanner(existingBanner.key, item);
            } else {
                viewModel.addBanner(item);
            }
            observeSuccess();
        }

        private void observeSuccess() {
            viewModel.getOperationSuccess().observe(this, success -> {
                if (success != null && success) {
                    dismiss();
                }
            });
        }

        private void openGallery() {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
                selectedImageUri = data.getData();
                if (ivPreview != null) {
                    ivPreview.setVisibility(View.VISIBLE);
                    ivPreview.setImageURI(selectedImageUri);
                }
                if (ivPreviewDetailed != null) {
                    ivPreviewDetailed.setVisibility(View.VISIBLE);
                    ivPreviewDetailed.setImageURI(selectedImageUri);
                }
            }
        }
    }
}
