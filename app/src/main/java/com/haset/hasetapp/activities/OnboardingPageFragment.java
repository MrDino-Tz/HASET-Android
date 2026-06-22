package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.haset.hasetapp.R;

public class OnboardingPageFragment extends Fragment {
    private static final String ARG_IMAGE = "image";
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESC = "desc";
    private static final String ARG_SHOW_LANG = "show_lang";

    public static OnboardingPageFragment newInstance(int imageRes, String title, String desc) {
        return newInstance(imageRes, title, desc, false);
    }

    public static OnboardingPageFragment newInstance(int imageRes, String title, String desc, boolean showLang) {
        Bundle args = new Bundle();
        args.putInt(ARG_IMAGE, imageRes);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESC, desc);
        args.putBoolean(ARG_SHOW_LANG, showLang);
        OnboardingPageFragment fragment = new OnboardingPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_page, container, false);
        ImageView image = view.findViewById(R.id.onboarding_image);
        TextView title = view.findViewById(R.id.onboarding_title);
        TextView desc = view.findViewById(R.id.onboarding_description);
        View langToggle = view.findViewById(R.id.languageToggle);

        Bundle args = getArguments();
        if (args != null) {
            image.setImageResource(args.getInt(ARG_IMAGE));
            title.setText(args.getString(ARG_TITLE));
            desc.setText(args.getString(ARG_DESC));
            
            if (args.getBoolean(ARG_SHOW_LANG, false)) {
                langToggle.setVisibility(View.VISIBLE);
                setupLanguageToggle(langToggle);
            } else {
                langToggle.setVisibility(View.GONE);
            }
        }
        return view;
    }

    private void setupLanguageToggle(View root) {
        com.haset.hasetapp.utils.LanguageToggleHelper.setup(getActivity(), root, languageCode -> {
            com.haset.hasetapp.utils.CustomDialog.showLoading(getContext(), getString(R.string.switching_language));
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                com.haset.hasetapp.utils.LocaleHelper.setLocale(getContext(), languageCode);
                com.haset.hasetapp.utils.PreferenceManager pm = new com.haset.hasetapp.utils.PreferenceManager(getContext());
                pm.setLanguage(languageCode);
                com.haset.hasetapp.utils.CustomDialog.hideLoading();
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            }, 500);
        });
    }
}
