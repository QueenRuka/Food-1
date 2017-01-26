package itsjustaaron.food;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.exceptions.BackendlessException;
import com.backendless.persistence.BackendlessDataQuery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class NewFood extends AppCompatActivity {

    private boolean imageUpdated;

    private Bitmap pic;

    private List<Food> searchResults;

    private ProgressDialog wait;

    private class foodAdapter extends ArrayAdapter<Food> {
        public List<Food> values;
        private Context context;

        public foodAdapter(Context context, List<Food> values) {
            super(context, -1, values);
            if (values == null) {
                values = new ArrayList<Food>();
            }
            this.values = values;
            this.context = context;
        }

        @Override
        public View getView(final int position, final View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.food_list_item, parent, false);
            ((ImageView) rowView.findViewById(R.id.foodImage)).setImageBitmap(BitmapFactory.decodeFile(Data.fileDir + "/foods/" + values.get(position).image));
            ((TextView) rowView.findViewById(R.id.foodName)).setText(values.get(position).name);
            ((TextView) rowView.findViewById(R.id.foodDescription)).setText(values.get(position).description);
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog ad = new AlertDialog.Builder(context)
                            .setTitle("Select this food?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialogInterface, int i) {
                                    new AsyncTask<Void, Void, Integer>() {
                                        @Override
                                        public void onPreExecute() {
                                            dialogInterface.dismiss();
                                            wait.show();
                                        }

                                        @Override
                                        public Integer doInBackground(Void... voids) {
                                            try {
                                                Map<String, String> craving = new HashMap<String, String>();
                                                craving.put("foodID", values.get(position).objectId);
                                                craving.put("numFollowers", "1");
                                                craving.put("ownerID", Data.user.getEmail());
                                                Map map = Backendless.Persistence.of("Craving").save(craving);
                                                Map<String, String> cravingFollower = new HashMap<String, String>();
                                                cravingFollower.put("userID", Data.user.getEmail());
                                                cravingFollower.put("cravingID", map.get("objectId").toString());
                                                Backendless.Persistence.of("cravingFollowers").save(cravingFollower);
                                            } catch (BackendlessException e) {
                                                Log.d("backendless", e.toString());
                                                return 1;
                                            }
                                            return 0;
                                        }

                                        @Override
                                        public void onPostExecute(Integer x) {
                                            if (x == 0) {
                                                Toast.makeText(context, "Succeeded", Toast.LENGTH_SHORT).show();
                                                finish();
                                            } else {
                                                Toast.makeText(context, "Error, please contact the developer if the problem persists", Toast.LENGTH_LONG).show();
                                            }
                                            dialogInterface.dismiss();
                                            wait.dismiss();
                                        }
                                    }.execute(new Void[]{});
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            }).create();
                    ad.show();
                }
            });
            return rowView;
        }
    };

    public void pickPhoto(View view) {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_food);
        searchResults = new ArrayList<>();
        ListView listView = (ListView) findViewById(R.id.searchFoodResult);
        foodAdapter adapter = new foodAdapter(this, searchResults);
        listView.setAdapter(adapter);
        wait = new ProgressDialog(this);
        wait.setMessage("Please Wait...");
        new AlertDialog.Builder(this).setTitle("Do you want to select a existing food or create a new one?")
                .setPositiveButton("Select", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                findViewById(R.id.searchFood).setVisibility(View.VISIBLE);
                dialogInterface.dismiss();
            }
        }).setNegativeButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                new AlertDialog.Builder(NewFood.this)
                        .setTitle("Caution")
                        .setMessage("Please make sure that you cannot find the food you are looking for before creating a new Food\n(Did you know you can enter tags of a food like \"noodle\" in the search?")
                        .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                findViewById(R.id.searchFood).setVisibility(View.GONE);
                                findViewById(R.id.createFood).setVisibility(View.VISIBLE);
                            }
                        })
                        .setNegativeButton("Go Back", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                findViewById(R.id.searchFood).setVisibility(View.VISIBLE);
                                dialog.dismiss();
                            }
                        }).show();
            }
        }).show();

        final LinearLayout container = (LinearLayout)findViewById(R.id.tagContainer);
        for(int i = 0; i < Data.tags.size(); i++) {
            String tag = Data.tags.get(i);
            final CheckedTextView checkedTextView = new CheckedTextView(this);
            checkedTextView.setText(tag);
            checkedTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(checkedTextView.isChecked()) {
                        checkedTextView.setCheckMarkDrawable(null);
                        checkedTextView.setChecked(false);
                    }else {
                        checkedTextView.setCheckMarkDrawable(ContextCompat.getDrawable(NewFood.this, R.drawable.ic_check));
                        checkedTextView.setChecked(true);
                    }
                }
            });
            checkedTextView.setChecked(false);
            checkedTextView.setId(i);
            container.addView(checkedTextView);
        }
        final Button button = new Button(this);
        button.setText("Add");

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(NewFood.this);
                final EditText input = new EditText(NewFood.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);
                alertDialog.setTitle("Enter your tag:");
                alertDialog.setPositiveButton("add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String tag = input.getText().toString().toUpperCase();
                        if(Data.tags.contains(tag)) {
                            Toast.makeText(NewFood.this, "There is already a tag with the same name!", Toast.LENGTH_LONG).show();
                        }else {
                            Data.tags.add(tag);
                            final CheckedTextView ctv = new CheckedTextView(NewFood.this);
                            ctv.setText(tag);
                            ctv.setChecked(true);
                            ctv.setId(Data.tags.size() - 1);
                            ctv.setCheckMarkDrawable(ContextCompat.getDrawable(NewFood.this, R.drawable.ic_check));
                            ctv.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if(ctv.isChecked()) {
                                        ctv.setCheckMarkDrawable(null);
                                        ctv.setChecked(false);
                                    }else {
                                        ctv.setCheckMarkDrawable(ContextCompat.getDrawable(NewFood.this, R.drawable.ic_check));
                                        ctv.setChecked(true);
                                    }
                                }
                            });
                            container.addView(ctv, container.getChildCount() - 1);
                        }
                    }
                }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                alertDialog.show();
            }
        });
        button.setWidth(container.getWidth());
        container.addView(button);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (data != null) {
                Uri image = data.getData();
                Intent cropIntent = new Intent("com.android.camera.action.CROP");
                cropIntent.setDataAndType(image, "image/*");
                cropIntent.putExtra("crop", "true");
                cropIntent.putExtra("aspectX", 10);
                cropIntent.putExtra("aspectY", 10);
                cropIntent.putExtra("outputX", 128);
                cropIntent.putExtra("outputY", 128);
                cropIntent.putExtra("return-data", true);
                startActivityForResult(cropIntent, 1);
            }
        } else {
            imageUpdated = true;
            pic = data.getExtras().getParcelable("data");
            ((ImageView) findViewById(R.id.profileImage)).setImageBitmap(pic);
        }
    }

    public void Create(View view) {
        ProgressDialog pd = new ProgressDialog(NewFood.this);
        pd.setMessage("Please wait...");
        pd.show();
        String name = ((EditText)findViewById(R.id.createFoodName)).getText().toString();
        String desc = ((EditText)findViewById(R.id.createFoodDesc)).getText().toString();
        try {
            File dest = new File(Data.fileDir + "/foods/" + name + ".png");
            OutputStream out = new FileOutputStream(dest);
            pic.compress(Bitmap.CompressFormat.PNG, 100, out);
            Food food = new Food();
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("name", name);
            hashMap.put("description", desc);
            hashMap.put("image", name + ".png");

        }catch (Exception e) {
            Toast.makeText(NewFood.this, "Error! If the problem persists, please contact the developer", Toast.LENGTH_LONG).show();
            Log.d("Save pic", e.toString());
        }

    }

    public void Search(View view) {
        searchResults.clear();

        String search = ((EditText) findViewById(R.id.searchFoodBox)).getText().toString();
        wait.show();
        List<String> tagResult = Food.csvToList(search);
        boolean tagCheck = true;
        for (int i = 0; i < tagResult.size(); i++) {
            if (!Data.tags.contains(tagResult.get(i))) {
                tagCheck = false;
                break;
            }
        }
        String whereClause = "";
        if (tagCheck) {
            for (int i = 0; i < tagResult.size(); i++) {
                whereClause = whereClause + "tags LIKE '%" + tagResult.get(i) + "%'";
                if (i != tagResult.size() - 1) {
                    whereClause = whereClause + " and ";
                }
            }
        } else {
            whereClause = "name LIKE '%" + search + "%'";
        }
        final BackendlessDataQuery dataQuery = new BackendlessDataQuery();
        dataQuery.setWhereClause(whereClause);
        new AsyncTask<Void, Void, Integer>() {
            @Override
            public Integer doInBackground(Void... voids) {
                try {
                    BackendlessCollection<Map> mapBackendlessCollection = Backendless.Persistence.of("Food").find(dataQuery);
                    final List<Map> maps = mapBackendlessCollection.getCurrentPage();
                    if (maps.size() != 0) {
                        for (int i = 0; i < maps.size(); i++) {
                            Map map = maps.get(i);
                            searchResults.add(new Food(map));
                        }
                        return 0;
                    } else {
                        return 1;
                    }
                } catch (BackendlessException e) {
                    Log.d("backendless", e.toString());
                    return 2;
                }
            }

            @Override
            public void onPostExecute(Integer i) {
                if (i == 0) {
                    ListView listView = (ListView) findViewById(R.id.searchFoodResult);
                    ((foodAdapter) listView.getAdapter()).notifyDataSetChanged();
                } else if (i == 1) {
                    Toast.makeText(NewFood.this, "Your search yields no results", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(NewFood.this, "Error: your search has failed, please contact the developer if the issue presists", Toast.LENGTH_LONG).show();
                }
                wait.dismiss();
            }
        }.execute(new Void[]{});
    }
}