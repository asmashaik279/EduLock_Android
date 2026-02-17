package com.example.edulock;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

public class ResourcesFragment extends Fragment {

    private MaterialCardView cardAimHigh, cardMeaningful, cardFormulae, cardSelfCare;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_resources, container, false);

        // Linking XML Cards
        cardAimHigh = view.findViewById(R.id.cardAimHigh);
        cardMeaningful = view.findViewById(R.id.cardMeaningful);
        cardFormulae = view.findViewById(R.id.cardFormulae);
        cardSelfCare = view.findViewById(R.id.cardSelfCare);

        // Click Listeners with your new file names
        cardAimHigh.setOnClickListener(v -> openPDF("AimHighTeen.pdf", "Aim High Teen"));
        
        cardMeaningful.setOnClickListener(v -> 
                openPDF("Being-Meaningful-Without-Being-Mean.pdf", "Being Meaningful"));

        cardFormulae.setOnClickListener(v -> 
                openPDF("IMPORTANT_FORMULAE_FOR_COMPETITIVE_EXAMS.pdf", "Exam Formulae"));

        cardSelfCare.setOnClickListener(v -> 
                openPDF("Taking-Care-of-Ourselves-Yet-Not-Being-Selfish-Guide.pdf", "Self Care Guide"));

        return view;
    }

    private void openPDF(String fileName, String title) {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), PDFViewActivity.class);
            intent.putExtra("fileName", fileName);
            intent.putExtra("title", title);
            startActivity(intent);
        }
    }
}
