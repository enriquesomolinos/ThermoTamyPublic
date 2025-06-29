package com.tiodev.vegtummy;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tiodev.vegtummy.Adapter.FavoriteListNameAdapter; // New Adapter
import com.tiodev.vegtummy.FavoriteList; // New POJO
import com.tiodev.vegtummy.FavoriteListManager; // New Manager

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment implements FavoriteListNameAdapter.OnFavoriteListClickListener {

    private RecyclerView favoritesRecyclerView;
    private FavoriteListNameAdapter listNameAdapter; // Changed adapter
    private List<FavoriteList> favoriteMasterList; // Changed data type
    private TextView emptyFavoritesText;
    private FloatingActionButton fabCreateList;

    // For context menu, to know which item was long-pressed
    private FavoriteList selectedFavoriteListForContextMenu;


    public FavoritesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        favoritesRecyclerView = view.findViewById(R.id.favorites_recycler_view);
        emptyFavoritesText = view.findViewById(R.id.empty_favorites_text);
        fabCreateList = view.findViewById(R.id.fab_create_list);

        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        favoriteMasterList = new ArrayList<>();
        listNameAdapter = new FavoriteListNameAdapter(getContext(), favoriteMasterList, this);
        favoritesRecyclerView.setAdapter(listNameAdapter);

        // Register RecyclerView for context menu (for long-press on items)
        registerForContextMenu(favoritesRecyclerView);

        fabCreateList.setOnClickListener(v -> showCreateListDialog());

        // No need for AppDatabase here anymore to load recipes directly into this fragment's main list
        // That will be handled when a specific list is selected to view its recipes.

        loadFavoriteLists(); // Load list names

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavoriteLists(); // Refresh list names on resume
    }

    private void loadFavoriteLists() {
        if (getContext() == null) return; // Context might be null if fragment is detached

        List<FavoriteList> currentLists = FavoriteListManager.getFavoriteLists(getContext());
        favoriteMasterList.clear();
        if (currentLists != null) {
            favoriteMasterList.addAll(currentLists);
        }

        if (favoriteMasterList.isEmpty()) {
            emptyFavoritesText.setVisibility(View.VISIBLE);
            favoritesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyFavoritesText.setVisibility(View.GONE);
            favoritesRecyclerView.setVisibility(View.VISIBLE);
        }
        if (listNameAdapter != null) {
            // The favoriteMasterList instance (which the adapter holds a reference to)
            // has been updated (cleared and new items added).
            // So, just notifying the adapter should be sufficient.
            listNameAdapter.notifyDataSetChanged();
        }
    }

    private void showCreateListDialog() {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.fab_create_list_content_description); // Reuses existing string

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint(R.string.hint_list_name);
        builder.setView(input);

        builder.setPositiveButton(R.string.button_create, (dialog, which) -> {
            String listName = input.getText().toString().trim();
            if (!listName.isEmpty()) {
                FavoriteList newList = FavoriteListManager.createFavoriteList(getContext(), listName);
                if (newList != null) {
                    loadFavoriteLists(); // Refresh the list
                    Toast.makeText(getContext(), getString(R.string.toast_list_created, listName), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), R.string.toast_failed_create_list, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), R.string.toast_list_name_empty, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    public void onFavoriteListClick(FavoriteList favoriteList) {
        if (getActivity() != null && favoriteList != null) {
            FavoriteRecipeListFragment recipeListFragment = FavoriteRecipeListFragment.newInstance(favoriteList.getId());

            // Perform fragment transaction
            // Assuming R.id.fragment_container is the ID of the FrameLayout in HomeActivity
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, recipeListFragment)
                    .addToBackStack(null) // Add to back stack so user can navigate back to the list of lists
                    .commit();
        } else {
            Toast.makeText(getContext(), R.string.toast_error_opening_list, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFavoriteListLongClick(FavoriteList favoriteList, View view) {
        selectedFavoriteListForContextMenu = favoriteList;
        // The view passed is the item view, which is already registered for context menu.
        // So, we just need to ensure the fragment's onCreateContextMenu is called.
        // The RecyclerView item should have android:longClickable="true" (default for ConstraintLayout with listener)
        if (getActivity() != null) {
            getActivity().openContextMenu(view); // Open context menu on the specific view
        }
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (getActivity() != null && selectedFavoriteListForContextMenu != null) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.favorite_list_context_menu, menu);
            menu.setHeaderTitle(getString(R.string.context_menu_title_manage_list, selectedFavoriteListForContextMenu.getName()));
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (selectedFavoriteListForContextMenu == null || getContext() == null) {
            return super.onContextItemSelected(item);
        }

        int itemId = item.getItemId();
        if (itemId == R.id.action_rename_list) {
            showRenameListDialog(selectedFavoriteListForContextMenu);
            return true;
        } else if (itemId == R.id.action_delete_list) {
            showDeleteListConfirmDialog(selectedFavoriteListForContextMenu);
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    private void showRenameListDialog(final FavoriteList listToRename) {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.dialog_title_rename_list);

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(listToRename.getName());
        input.setHint(R.string.hint_list_name); // Also good to have a hint here
        input.setSelection(input.getText().length()); // Cursor at the end
        builder.setView(input);

        builder.setPositiveButton(R.string.button_rename, (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(listToRename.getName())) {
                if (FavoriteListManager.renameFavoriteList(getContext(), listToRename.getId(), newName)) {
                    loadFavoriteLists(); // Refresh
                    Toast.makeText(getContext(), getString(R.string.toast_list_renamed, newName), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), R.string.toast_failed_rename_list, Toast.LENGTH_SHORT).show();
                }
            } else if (newName.isEmpty()) {
                Toast.makeText(getContext(), R.string.toast_list_name_empty, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteListConfirmDialog(final FavoriteList listToDelete) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_title_delete_list)
                .setMessage(getString(R.string.dialog_message_delete_list_confirm, listToDelete.getName()))
                .setPositiveButton(R.string.button_delete, (dialog, which) -> {
                    if (FavoriteListManager.deleteFavoriteList(getContext(), listToDelete.getId())) {
                        loadFavoriteLists(); // Refresh
                        Toast.makeText(getContext(), getString(R.string.toast_list_deleted, listToDelete.getName()), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), R.string.toast_failed_delete_list, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
