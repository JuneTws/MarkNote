package me.shouheng.notepal.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import me.shouheng.notepal.R;
import me.shouheng.notepal.activity.ContentActivity;
import me.shouheng.notepal.config.Constants;
import me.shouheng.notepal.databinding.FragmentNoteViewBinding;
import me.shouheng.notepal.model.Attachment;
import me.shouheng.notepal.model.ModelFactory;
import me.shouheng.notepal.model.Note;
import me.shouheng.notepal.provider.AttachmentsStore;
import me.shouheng.notepal.util.AttachmentHelper;
import me.shouheng.notepal.util.ColorUtils;
import me.shouheng.notepal.util.LogUtils;
import me.shouheng.notepal.util.ModelHelper;
import me.shouheng.notepal.util.PrintUtils;
import me.shouheng.notepal.util.ToastUtils;

/**
 * Created by wangshouheng on 2017/5/13.*/
public class NoteViewFragment extends BaseFragment<FragmentNoteViewBinding> {

    private final int REQUEST_FOR_EDIT = 0x01;

    private Note note;
    private String content;

    private boolean isPreview = false;
    private boolean isContentChanged = false;

    public static NoteViewFragment newInstance(Note note, Integer position, Integer requestCode){
        Bundle arg = new Bundle();
        arg.putSerializable(Constants.EXTRA_MODEL, note);
        if (position != null) arg.putInt(Constants.EXTRA_POSITION, position);
        if (requestCode != null) arg.putInt(Constants.EXTRA_REQUEST_CODE, requestCode);
        NoteViewFragment fragment = new NoteViewFragment();
        fragment.setArguments(arg);
        return fragment;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_note_view;
    }

    @Override
    protected void doCreateView(Bundle savedInstanceState) {
        handleArguments();

        configToolbar();

        configViews();

        refreshLayout(false);
    }

    private void handleArguments() {
        Bundle arguments = getArguments();
        if (arguments.containsKey(Constants.EXTRA_MODEL)){
            note = (Note) arguments.getSerializable(Constants.EXTRA_MODEL);
        }

        if (TextUtils.isEmpty(note.getContent())) {
            Attachment noteFile = AttachmentsStore.getInstance(getContext()).get(note.getContentCode());
            LogUtils.d("noteFile: " + noteFile);
            if (noteFile == null) {
                ToastUtils.makeToast(getContext(), R.string.note_failed_to_get_note_content);
                return;
            }
            File file = new File(noteFile.getPath());
            LogUtils.d("file: " + file);
            try {
                content = FileUtils.readFileToString(file, "utf-8");
            } catch (IOException e) {
                LogUtils.d("IOException: " + e);
                ToastUtils.makeToast(getContext(), R.string.note_failed_to_read_file);
            }
            note.setContent(content);
        } else {
            isPreview = true;
        }
    }

    private void configToolbar() {
        ((AppCompatActivity) getActivity()).setSupportActionBar(getBinding().toolbar);
        final ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(false);
        if (!isDarkTheme()) getBinding().toolbar.setPopupTheme(R.style.AppTheme_PopupOverlay);
    }

    private void configViews() {
        getBinding().mdView.setWebViewTheme(isDarkTheme());
        getBinding().mdView.setPrimaryColor(primaryColor());
        getBinding().mdView.setPrimaryDark(ColorUtils.calStatusBarColor(primaryColor()));
        getBinding().mdView.setOnImageClickedListener((url, urls) -> {
            List<Attachment> attachments = new ArrayList<>();
            Attachment clickedAttachment = null;
            for (String u : urls) {
                Attachment attachment = getAttachmentFormUrl(u);
                attachments.add(attachment);
                if (u.equals(url)) clickedAttachment = attachment;
            }
            AttachmentHelper.resolveClickEvent(getContext(),
                    clickedAttachment, attachments, note.getTitle());
        });
    }

    private void refreshLayout(boolean reload){
        if (!TextUtils.isEmpty(note.getContent())) {
            if (reload) {
                getBinding().mdView.parseMarkdown(note.getContent(), true);
            } else {
                getBinding().mdView.setOnLoadingFinishListener(() ->
                        getBinding().mdView.parseMarkdown(note.getContent(), true));
            }
        }

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(note.getTitle());
    }

    private Attachment getAttachmentFormUrl(String url) {
        Uri uri = Uri.parse(url);
        Attachment attachment = ModelFactory.getAttachment(getContext());
        attachment.setUri(uri);
        attachment.setMineType(Constants.MIME_TYPE_IMAGE);
        return attachment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(!isPreview);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.note_view_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_edit:
                ContentActivity.startNoteEditForResult(this, note, null, REQUEST_FOR_EDIT);
                break;
            case R.id.action_share:
                ModelHelper.share(getContext(), note.getTitle(), content, new ArrayList<>());
                break;
            case R.id.capture:
                createWebCapture(getBinding().mdView);
                break;
            case R.id.print:
                PrintUtils.print(getContext(), getBinding().mdView, note);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        assert getActivity() != null;
        if (isPreview) {
            getActivity().finish();
        } else {
            if (isContentChanged) {
                if (getArguments().containsKey(Constants.EXTRA_REQUEST_CODE)) {
                    Intent intent = new Intent();
                    intent.putExtra(Constants.EXTRA_MODEL, (Serializable) note);
                    if (getArguments().containsKey(Constants.EXTRA_POSITION)){
                        intent.putExtra(Constants.EXTRA_POSITION, getArguments().getInt(Constants.EXTRA_POSITION, 0));
                    }
                    getActivity().setResult(Activity.RESULT_OK, intent);
                }
            }
            getActivity().finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_FOR_EDIT:
                if (resultCode == Activity.RESULT_OK){
                    isContentChanged = true;
                    note = (Note) data.getSerializableExtra(Constants.EXTRA_MODEL);
                    refreshLayout(true);
                }
                break;
        }
    }
}