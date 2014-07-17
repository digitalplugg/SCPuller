package de.dakror.scpuller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.Mp3File;

public class SCPuller extends Activity
{
	public static final String cID = "da71cf8b29a850e6d978adc9f502763e";
	
	private int userID;
	private LinearLayout ll;
	private String userName;
	private String intentSong;
	JSONArray allSongs;
	ArrayList<Integer> downloadedSongs = new ArrayList<Integer>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scpuller);
		// -- Check if from intent -- //
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();
		if (action.equals(Intent.ACTION_SEND) && type != null && type.equals("text/plain"))
		{
			this.intentSong = intent.getStringExtra(Intent.EXTRA_TEXT);
			String user = this.intentSong.substring(0, this.intentSong.lastIndexOf("/"));
			EditText link = (EditText) SCPuller.this.findViewById(R.id.link);
			link.setText(user);
			new JSONHelper().execute();
		}
		// -- //
		final Button find = (Button) findViewById(R.id.find);
		find.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				new JSONHelper().execute();
			}
		});
		
		loadDownloadedSongs();
	}
	
	public void loadDownloadedSongs()
	{
		File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "SC Puller/downloaded.txt");
		if (!f.exists()) return;
		else
		{
			try
			{
				downloadedSongs.clear();
				BufferedReader br = new BufferedReader(new FileReader(f));
				String line = "";
				while ((line = br.readLine()) != null)
					downloadedSongs.add(Integer.parseInt(line));
				
				br.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void saveDownloadedSongs()
	{
		File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "SC Puller/downloaded.txt");
		try
		{
			f.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			for (int i : downloadedSongs)
				bw.write(i + "\r\n");
			
			bw.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void proceedFindClick(String result)
	{
		if (result == null)
		{
			Toast.makeText(this, "Either this link is not valid or you aren't connected to the internet!", Toast.LENGTH_LONG).show();
			return;
		}
		final ScrollView songsScrollView = (ScrollView) findViewById(R.id.songsScrollView);
		ProgressDialog pd = ProgressDialog.show(this, "", "Loading songs. Please wait...", false);
		
		this.allSongs = null;
		
		try
		{
			this.allSongs = new JSONArray(result);
		}
		catch (JSONException e1)
		{
			e1.printStackTrace();
			System.exit(0);
		}
		
		pd.dismiss();
		if (this.allSongs == null)
		{
			return;
		}
		else if (this.allSongs.length() == 0)
		{
			Toast.makeText(this, "This user has no public songs yet!", Toast.LENGTH_LONG).show();
		}
		songsScrollView.removeAllViews();
		this.ll = new LinearLayout(this);
		this.ll.setOrientation(LinearLayout.VERTICAL);
		
		for (int i = 0; i < this.allSongs.length(); i++)
		{
			try
			{
				JSONObject o = this.allSongs.getJSONObject(i);
				CheckBox song = new CheckBox(this);
				song.setText(o.getString("title"));
				song.setChecked(!downloadedSongs.contains(o.getInt("id")));
				if (downloadedSongs.contains(o.getInt("id"))) song.setBackgroundColor(Color.GRAY);
				this.ll.addView(song);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		songsScrollView.addView(this.ll);
		Button selectall = (Button) findViewById(R.id.selectall);
		selectall.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (ll == null) return;
				int childcount = ll.getChildCount();
				for (int i = 0; i < childcount; i++)
				{
					CheckBox cb = (CheckBox) ll.getChildAt(i);
					cb.setChecked(true);
				}
			}
		});
		Button selectnone = (Button) findViewById(R.id.selectnone);
		selectnone.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (ll == null) return;
				int childcount = ll.getChildCount();
				for (int i = 0; i < childcount; i++)
				{
					CheckBox cb = (CheckBox) ll.getChildAt(i);
					cb.setChecked(false);
				}
			}
		});
		Button download = (Button) findViewById(R.id.download);
		download.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				new DownloadHelper().execute(null, null);
			}
		});
		if (this.intentSong != null)
		{
			// -- deselect all -- //
			int childcount = this.ll.getChildCount();
			for (int i = 0; i < childcount; i++)
			{
				CheckBox cb = (CheckBox) this.ll.getChildAt(i);
				cb.setChecked(false);
			}
			// -- select intented -- //
			try
			{
				for (int i = 0; i < this.allSongs.length(); i++)
				{
					JSONObject song = this.allSongs.getJSONObject(i);
					if (song.getString("permalink_url").equals(this.intentSong))
					{
						CheckBox cb = (CheckBox) this.ll.getChildAt(i);
						cb.setChecked(true);
					}
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	class JSONHelper extends AsyncTask<String, Void, String>
	{
		ProgressDialog pd;
		
		@Override
		protected void onPreExecute()
		{
			pd = ProgressDialog.show(SCPuller.this, "", "Loading songs. Please wait...", true);
		}
		
		@Override
		protected String doInBackground(String... params)
		{
			try
			{
				String content = getFileContents(new URL("http://api.soundcloud.com/resolve.json?client_id=" + SCPuller.cID + "&url=" + ((EditText) SCPuller.this.findViewById(R.id.link)).getText().toString()));
				JSONObject user = new JSONObject(content);
				SCPuller.this.userID = user.getInt("id");
				SCPuller.this.userName = user.getString("username");
				String result = getFileContents(new URL("http://dakror.de/AndroidSCPuller/getallsongs.php?uid=" + SCPuller.this.userID));
				return result;
			}
			catch (Exception e)
			{
				return null;
			}
		}
		
		protected String getFileContents(URL u) throws IOException
		{
			String res = "", line = "";
			BufferedReader br = new BufferedReader(new InputStreamReader(u.openStream()));
			while ((line = br.readLine()) != null)
			{
				res += line;
			}
			br.close();
			return res;
		}
		
		@Override
		protected void onPostExecute(String result)
		{
			super.onPostExecute(result);
			pd.dismiss();
			SCPuller.this.proceedFindClick(result);
		}
	}
	
	class DownloadHelper extends AsyncTask<Void, Integer, Void>
	{
		ProgressDialog pd;
		long totalSize = 0;
		long totalProg = 0;
		
		@SuppressLint("NewApi")
		@Override
		protected void onPreExecute()
		{
			pd = new ProgressDialog(SCPuller.this);
			pd.setMessage("Downloading songs. Please wait..");
			pd.setProgressNumberFormat(null);
			pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pd.setMax(100);
			pd.setIndeterminate(true);
			pd.setCanceledOnTouchOutside(false);
			pd.show();
		}
		
		@Override
		protected Void doInBackground(Void... params)
		{
			File parent = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
			File dest = new File(parent, "SC Puller/" + SCPuller.this.userName);
			dest.mkdirs();
			if (SCPuller.this.ll == null) return null;
			int childcount = SCPuller.this.ll.getChildCount();
			for (int i = 0; i < childcount; i++)
			{
				if (!((CheckBox) SCPuller.this.ll.getChildAt(i)).isChecked()) continue;
				try
				{
					this.totalSize += new URL(SCPuller.this.allSongs.getJSONObject(i).getString("stream_url") + "?client_id=" + cID).openConnection().getContentLength();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			pd.setIndeterminate(false);
			for (int i = 0; i < childcount; i++)
			{
				CheckBox cb = (CheckBox) SCPuller.this.ll.getChildAt(i);
				if (cb.isChecked())
				{
					File ftemp = new File(dest, cb.getText().toString().replaceAll("[^\\w ]", "") + ".mp3.tmp");
					File file = new File(dest, cb.getText().toString().replaceAll("[^\\w ]", "") + ".mp3");
					try
					{
						JSONObject d = SCPuller.this.allSongs.getJSONObject(i);
						ftemp.createNewFile();
						String stream_url = d.getString("stream_url");
						if (stream_url.length() > 0)
						{
							download(new URL(stream_url + "?client_id=" + cID), new FileOutputStream(ftemp));
							
							downloadedSongs.add(d.getInt("id"));
							
							Mp3File f = new Mp3File(ftemp.getPath());
							ID3v2 id3v2;
							if (f.hasId3v2Tag())
							{
								id3v2 = f.getId3v2Tag();
							}
							else
							{
								id3v2 = new ID3v24Tag();
								f.setId3v2Tag(id3v2);
							}
							id3v2.setAlbumArtist(d.getJSONObject("user").getString("username"));
							id3v2.setArtist(d.getJSONObject("user").getString("username"));
							id3v2.setAlbum(d.getString("title"));
							id3v2.setComment(d.getString("description"));
							id3v2.setOriginalArtist(d.getJSONObject("user").getString("username"));
							id3v2.setTitle(d.getString("title"));
							try
							{
								id3v2.setYear("" + d.getInt("release_year"));
							}
							catch (Exception e)
							{}
							try
							{
								String artwork = (d.has("artwork_url")) ? d.getString("artwork_url") : null;
								
								if (artwork != null && !artwork.equals("null"))
								{
									ByteArrayOutputStream baos = new ByteArrayOutputStream();
									copyInputStream(new URL(artwork.replace("large", "t500x500")).openStream(), baos);
									id3v2.setAlbumImage(baos.toByteArray(), "image/jpeg");
								}
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
							f.save(file.getPath());
							ftemp.delete();
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
			return null;
		}
		
		public synchronized void download(URL in, OutputStream out) throws Exception
		{
			InputStream is = in.openStream();
			byte[] buffer = new byte[2048];
			int len = is.read(buffer);
			int prog = 0;
			int fullsize = in.openConnection().getContentLength();
			while (prog != fullsize)
			{
				prog += len;
				out.write(buffer, 0, len);
				len = is.read(buffer);
				publishProgress((int) (((totalProg + prog) / (double) totalSize) * 100));
			}
			totalProg += fullsize;
			is.close();
			out.close();
		}
		
		public synchronized void copyInputStream(InputStream is, OutputStream out) throws Exception
		{
			byte[] buffer = new byte[2048];
			int len = is.read(buffer);
			while (len != -1)
			{
				out.write(buffer, 0, len);
				len = is.read(buffer);
			}
			is.close();
			out.close();
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress)
		{
			pd.setProgress(progress[0]);
		}
		
		@Override
		protected void onPostExecute(Void result)
		{
			super.onPostExecute(result);
			
			saveDownloadedSongs();
			
			pd.dismiss();
		}
	}
}
