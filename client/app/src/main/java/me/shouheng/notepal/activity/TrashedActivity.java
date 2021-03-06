package me.shouheng.notepal.activity;

import android.support.v4.app.Fragment;

import me.shouheng.notepal.R;
import me.shouheng.notepal.activity.base.BaseListActivity;
import me.shouheng.notepal.fragment.CategoriesFragment;
import me.shouheng.notepal.fragment.NotesFragment;
import me.shouheng.notepal.model.Category;
import me.shouheng.notepal.model.Notebook;
import me.shouheng.notepal.model.enums.Status;
import me.shouheng.notepal.util.FragmentHelper;

/**
 * Created by wangshouheng on 2017/10/10.*/
public class TrashedActivity extends BaseListActivity {

    @Override
    protected CharSequence getActionbarTitle() {
        return getString(R.string.drawer_menu_trash);
    }

    @Override
    protected Fragment getNotesFragment() {
        return NotesFragment.newInstance(Status.TRASHED);
    }

    @Override
    protected Fragment getCategoryFragment() {
        return CategoriesFragment.newInstance(Status.TRASHED);
    }

    @Override
    public void onCategorySelected(Category category) {
        NotesFragment notesFragment = NotesFragment.newInstance(category, Status.TRASHED);
        FragmentHelper.replaceWithCallback(this, notesFragment, R.id.fragment_container);
    }

    @Override
    public void onNotebookSelected(Notebook notebook) {
        NotesFragment notesFragment = NotesFragment.newInstance(notebook, Status.TRASHED);
        FragmentHelper.replaceWithCallback(this, notesFragment, R.id.fragment_container);
    }
}
