package com.example.smartpantrymanager.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.R;
import com.example.smartpantrymanager.util.RecipeMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the Suggested Recipes screen. It renders two view types from one list:
 * section headers (String) and recipe cards (RecipeMatcher.Match), which is how the
 * strict "Ready to cook" list is kept visually separate from the optional
 * "Almost there" list.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_RECIPE = 1;

    public interface OnRecipeClickListener {
        void onRecipeClick(long recipeId);
    }

    private final List<Object> rows = new ArrayList<>();
    private final OnRecipeClickListener listener;

    public RecipeAdapter(OnRecipeClickListener listener) {
        this.listener = listener;
    }

    public void setRows(List<Object> newRows) {
        rows.clear();
        if (newRows != null) {
            rows.addAll(newRows);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position) instanceof String ? TYPE_HEADER : TYPE_RECIPE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_header, parent, false));
        }
        return new RecipeViewHolder(inflater.inflate(R.layout.item_recipe, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object row = rows.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvHeader.setText((String) row);
        } else {
            ((RecipeViewHolder) holder).bind((RecipeMatcher.Match) row);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvHeader;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvHeader);
        }
    }

    private class RecipeViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvMeta;
        private final TextView tvMissing;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRecipeName);
            tvMeta = itemView.findViewById(R.id.tvRecipeMeta);
            tvMissing = itemView.findViewById(R.id.tvMissing);
        }

        void bind(final RecipeMatcher.Match match) {
            tvName.setText(match.recipe.getName());
            tvMeta.setText(itemView.getContext().getString(R.string.label_prep_time, match.recipe.getPrepMinutes())
                    + "  |  " + itemView.getContext().getString(R.string.label_serves, match.recipe.getServings()));

            if (match.canCook()) {
                tvMissing.setVisibility(View.GONE);
            } else {
                tvMissing.setVisibility(View.VISIBLE);
                tvMissing.setText(itemView.getContext().getString(R.string.label_missing, match.missingNames()));
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onRecipeClick(match.recipe.getId());
                }
            });
        }
    }
}
