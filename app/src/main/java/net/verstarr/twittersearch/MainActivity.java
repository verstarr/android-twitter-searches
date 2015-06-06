package net.verstarr.twittersearch;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;


public class MainActivity extends ListActivity {

    private static final String SEARCHES = "searches";

    private EditText queryEditText; // user's query search
    private EditText tagEditText; // user's query tag label
    private SharedPreferences savedSearches; // user's searches
    private ArrayList<String> tags; // list of user's tags for saved searches
    private ArrayAdapter<String> adapter; // binds tags to ListView
    private ImageButton saveButton; // save button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // to reference ui edit texts
        queryEditText = (EditText) findViewById(R.id.queryEditText);
        tagEditText = (EditText) findViewById(R.id.tagEditText);

        // Shared preferences to save user tags
        savedSearches = getSharedPreferences(SEARCHES, MODE_PRIVATE);

        // store saved tags in the tags ArrayList
        tags = new ArrayList<String>(savedSearches.getAll().keySet());
        Collections.sort(tags, String.CASE_INSENSITIVE_ORDER);

        // Utilize the adaptor to join the key value pairs
        adapter = new ArrayAdapter<String>(this, R.layout.list_item, tags);
        setListAdapter(adapter);

        // register the listener to save/edit search
        saveButton = (ImageButton) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(saveButtonListener);

        // register the listener to search Twitter when user picks a tag
        getListView().setOnItemClickListener(itemClickListener);

        // set listener for user to delete/edit search
        getListView().setOnItemLongClickListener(itemLongClickListener);
    }

    // saveButtonListener
    public OnClickListener saveButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // create a new tag with queryEditText and tagEditText Values, not null
            if (queryEditText.getText().length() > 0 && tagEditText.getText().length() > 0) {
                addTaggedSearch(queryEditText.getText().toString(), tagEditText.getText().toString());
                queryEditText.setText(""); // clears the editText field after saved
                tagEditText.setText("");

                // Hide keyboard after save
                ((InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE)).hideSoftInputFromInputMethod(
                        tagEditText.getWindowToken(), 0);
            }
            // in the case that an editText field is empty
            else {
                // Create dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(R.string.missingMessage);
                builder.setPositiveButton(R.string.understand, null);

                AlertDialog errorDialog = builder.create();
                errorDialog.show();
            }
        }
    };

    public OnItemClickListener itemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // get query and make a URL representation search for Twitter
            String tag = ((TextView) view).getText().toString();
            String urlString = getString(R.string.searchURL) +
                    Uri.encode(savedSearches.getString(tag, ""), "UTF-8");

            // create an intent to send the search to a browser
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
            startActivity(webIntent); // Launches browsers with url
        }
    };

    public OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            // get string for long touched tag
            final String tag = ((TextView) view).getText().toString();

            // alert the user
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(getString(R.string.sharedEditDelTitle, tag));

            // set list of items in dialog
            builder.setItems(R.array.dialog_items, new DialogInterface.OnClickListener() {
                // respond to user touch with options
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: // share
                            shareSearch(tag);
                            break;
                        case 1: // edit
                            tagEditText.setText(tag);
                            queryEditText.setText(savedSearches.getString(tag, ""));
                            break;
                        case 2: // delete
                            deleteSearch(tag);
                            break;
                    }
                }
            });

            // set negative button
            builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                // for cancel button
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

            builder.create().show();
            return true;
        }
    };

    private void addTaggedSearch(String query, String tag) {
        SharedPreferences.Editor prefEditor = savedSearches.edit();
        prefEditor.putString(tag, query); // stores the search
        prefEditor.apply(); //commit stored search

        // if new tag, add and sort
        if (!tags.contains(tag)) {
            tags.add(tag);
            Collections.sort(tags, String.CASE_INSENSITIVE_ORDER);
            adapter.notifyDataSetChanged(); // rebinds tags to ListView
        }
    }

    private void shareSearch(String tag) {
        // create string to represent the url
        String urlString = getString(R.string.searchURL) +
                Uri.encode(savedSearches.getString(tag, ""), "UTF-8");

        // create Intent to share urlString
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shareSubject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.shareMessage));
        shareIntent.setType("text/plain");

        // display apps that can share this text
        startActivity(Intent.createChooser(shareIntent, getString(R.string.shareSearch)));
    }

    private void deleteSearch(final String tag) {
        // create alert
        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);

        // set alert message
        confirmBuilder.setMessage(getString(R.string.confirmMessage, tag));

        // set negative button
        confirmBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            // when cancel is clicked
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        // set positive button
        confirmBuilder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
            // when delete is clicked
            public void onClick(DialogInterface dialog, int id) {
                tags.remove(tag);

                // remove from sharedpref
                SharedPreferences.Editor prefEditor = savedSearches.edit();
                prefEditor.remove(tag); // remove search
                prefEditor.apply(); // commit change

                // rebind tags from ArrayList to ListView
                adapter.notifyDataSetChanged();
            }
        });

        // display dialog
        confirmBuilder.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
