package com.saidalattrach.sudokusolver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class Grid implements Runnable
{
	private static final float relGridSize = 0.975f;
	private static final float relSpacing = 0.002f;
	private static final float blockSpacingFactor = 8.0f;
	private static final float animTime = 1.0f;
	private static final float animDelay = 0.075f;
	private static final float shakeAnimTime = 0.4f;
	private static final float relAmplitude = 0.15f;
	private static final float frequency = 6.0f;
	private static final int bgColor = 0xFF303030;
	private static final int cellColor = 0xFFFFFFFF;
	private static final int selectColor = 0xFFB8B8B8;
	private static final int keyPadColor = 0xFF808080;
	private static final int keyPadNumColor = 0xFF000000;
	private static final int solvedNumColor = 0xFF1F75FE;
	
	private int gridSize;
	private int cellSize;
	private int spacing;
	private int blockSpacing;
	private int keyPadY;
	private int gridX;
	private int gridY;
	private int solveX;
	private int solveY;
	private int solveWidth;
	private int solveHeight;
	private int binX;
	private int binY;
	private int binWidth;
	private int binHeight;
	
	private int amplitude;
	
	private int[] grid = new int[9 * 9];
	private boolean[] redCells = new boolean[9 * 9];
	private boolean[] solvedCells = new boolean[9 * 9];

	private int selectedNumber = -1;
	private int selectedIndex = -1;
	
	//TODO: Fix potential threading problems
	private volatile boolean running = false;
	private volatile boolean surfaceValid = false;	
	private float inputX;
	private float inputY;
	private int inputEvent;
		
	private Thread thread;
	private Object lock = new Object();
	private Context context;
	private Typeface font;
	private SurfaceHolder holder;
	private Paint paint;
	
	private Bitmap recycleBin;
	
	public Grid(SurfaceHolder holder, Context context)
	{
		this.holder = holder;
		this.context = context;
	}
	
	public void start()
	{
		surfaceValid = true;
		running = true;
		thread = new Thread(this);
		thread.start();
	}
	
	public void stop()
	{
		surfaceValid = false;
		running = false;

		while (true) {
			try {
				thread.join();
				break;
			} catch (Exception e) {}
		}
	}
	
	public synchronized void input(float x, float y, int e)
	{
		synchronized (lock)
		{
			inputX = x;
			inputY = y;
			inputEvent = e;
		}
	}
	
	public void run()
	{
		init();
		
		Canvas canvas = null;
		
		float inX = -1;
		float inY = -1;
		int inEvent = -1;
		
		while (running)
		{
			synchronized (lock)
			{
				inX = inputX;
				inY = inputY;
				inEvent = inputEvent;
			
				inputX = -1;
				inputY = -1;
				inputEvent = -1;
			}
			
			if (inEvent != -1)
			{
				processInput(inX, inY, inEvent);

				if (selectedIndex != -1 && selectedNumber != -1 && selectedNumber != 10)
					placeNumber(selectedIndex, selectedNumber);

				render();
				
				selectedNumber = -1;
			}
		}
	}
	
	private void init()
	{
		int i, j, k;
		int left, top;
		int height;
		int screenWidth = 0;
		int screenHeight = 0;
		Canvas c = null;
		
		for (i = 0; i < 81; i++)
			grid[i] = 0;
		
		while (running)
		{
			c = lockCanvas();
			if (c != null)
			{
				screenWidth = c.getWidth();
				screenHeight = c.getHeight();
				holder.unlockCanvasAndPost(c);
				break;
			}
		}
		
		calculateDimensions(screenWidth);
		height = gridSize + 6 * blockSpacing + 4 * cellSize;
		if (screenHeight < height)
			calculateDimensions(screenWidth * screenHeight / height);
					
		gridX = (screenWidth - gridSize) / 2;
		gridY = (screenHeight - gridSize - cellSize - blockSpacing) / 2;
		keyPadY = gridY + gridSize + (screenHeight - gridY - gridSize - 2 * cellSize - blockSpacing) / 2;
		solveX = gridX + 3 * cellSize + 2 * spacing + blockSpacing;
		solveY = (gridY - cellSize) / 2;
		solveWidth = 3 * cellSize + 2 * spacing;
		solveHeight = cellSize;
		
		amplitude = (int) (relAmplitude * solveWidth);
		
		while (running)
		{
			try
			{
				font = Typeface.createFromAsset(context.getAssets(), "helvetica-neue-thin.otf");
				recycleBin = BitmapFactory.decodeStream(context.getAssets().open("recycle-bin.png"));
				
				binWidth = cellSize;
				binHeight = recycleBin.getHeight() * binWidth / recycleBin.getWidth();
				binX = screenWidth - binWidth;
				binY = (int) (0.005f * screenHeight);
	
				recycleBin = Bitmap.createScaledBitmap(recycleBin, binWidth, binHeight, true);
				c = new Canvas(recycleBin);
				c.drawColor(cellColor, Mode.SRC_ATOP);
				break;
			}
			catch (Exception e) {}
		}
				
		Bitmap cell = Bitmap.createBitmap(cellSize, cellSize, Bitmap.Config.ARGB_8888);
		Bitmap keyPad = Bitmap.createBitmap(gridSize, 2 * cellSize + blockSpacing, Bitmap.Config.ARGB_8888);
		Bitmap solveBtn = Bitmap.createBitmap(solveWidth, cellSize, Bitmap.Config.ARGB_8888);
		
		paint = new Paint();
		paint.setTypeface(font);
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setAntiAlias(true);
		setTextSize("0", 0.5f * cellSize, paint);

		c = new Canvas(cell);
		paint.setColor(cellColor);
		c.drawRect(0, 0, cellSize, cellSize, paint);
		
		// Draw key pad rectangles
		c = new Canvas(keyPad);
		paint.setColor(keyPadColor);
		
		left = 0;
		top = cellSize + blockSpacing;

		for (i = 0; i < 3; i++)
		{
			for (j = 0; j < 3; j++)
			{
				c.drawRect(left, top, left + cellSize, top + cellSize, paint);
				left += cellSize + spacing;
			}
			left += blockSpacing - spacing;
		}
		
		// Draw clear button on key pad
		paint.setColor(keyPadColor);
		left = 3 * cellSize + 2 * spacing + blockSpacing;
		c.drawRect(left, 0, left + 3 * cellSize + 2 * spacing, cellSize, paint);
		
		paint.setColor(keyPadNumColor);
		c.drawText("CLEAR", 9 * cellSize / 2 + 3 * spacing + blockSpacing, 3 * cellSize / 4, paint);
		
		// Draw numbers on key pad
		int counter = 1;
		
		left = cellSize / 2;
		top = cellSize + blockSpacing + 3 * cellSize / 4;
		
		for (i = 0; i < 3; i++)
		{
			for (j = 0; j < 3; j++)
			{
				c.drawText(String.valueOf(counter), left, top, paint);
				counter++;
				left += cellSize + spacing;
			}
			
			left += blockSpacing - spacing;
		}
		
		//Draw solve button
		c = new Canvas(solveBtn);
		c.drawColor(keyPadColor);
		c.drawText("SOLVE", solveBtn.getWidth() / 2, 3 * solveBtn.getHeight() / 4, paint);
		
		// Intro Animation
		int[] xi = new int[9];
		int[] xf = new int[9];
		int[] x = new int[9];
		
		int sby = 0;
		int kpy = 0;
		int bby = 0;
		
		counter = 0;
		int finalX = gridX;
		
		for (i = 0; i < 3; i++)
		{
			for (j = 0; j < 3; j++)
			{
				xf[counter] = finalX;
				finalX += cellSize + spacing;
				counter++;
			}
			finalX += blockSpacing - spacing;
		}
		
		int deltaX = xf[8] + cellSize;
		
		for (i = 0; i < 9; i++)
			xi[i] = xf[i] - deltaX;
		
		boolean running = true;
		
		long accumulator = 0L;
		long frameTime = 1000000000L / 60;
		long now = System.nanoTime();
		long lastTime = now;
		
		float t = 0.0f;
		float delta = frameTime / 1000000000.0f;
		
		int reflectionOffset = screenWidth - cellSize;
		if (cellSize % 2 != screenHeight % 2)
			reflectionOffset--;
		
		paint.setColor(cellColor);
		
		while (running)
		{	
			now = System.nanoTime();
			accumulator += now - lastTime;
			lastTime = now;
			
			if (accumulator >= frameTime)
			{	
				accumulator -= frameTime * (accumulator / frameTime);
				
				float fraction;
				
				for (i = 0; i < 9; i++)
				{
					fraction = (t - (8 - i) * animDelay) / (animTime - 8 * animDelay);
					x[i] = (int) (deltaX * fraction * (2 - fraction) + xi[i]);
					if (fraction >= 1)
					{
						if (i == 0)
							running = false;
						x[i] = xf[i];
					}
				}
				
				fraction = t / animTime;

				sby = (int) ((solveY + cellSize) * fraction * (2 - fraction) - cellSize);
				kpy = (int) ((keyPadY - screenHeight) * fraction * (2 - fraction) + screenHeight);
				bby = (int) ((binHeight + binY) * fraction * (2 - fraction) - binHeight);

				if (fraction >= 1)
				{
					sby = solveY;
					kpy = keyPadY;
				}
					
				t += delta;
				
				c = null;
				if (surfaceValid) c = holder.lockCanvas();
				if (c != null)
				{
					c.drawColor(bgColor);
					
					top = gridY;
					boolean fromRight = false;
					
					for (i = 0; i < 3; i++)
					{
						for (j = 0; j < 3; j++)
						{
							for (k = 0; k < 9; k++)
							{
								left = x[k];		
								if (fromRight)
									left = reflectionOffset - left;
								c.drawRect(left, top, left + cellSize, top + cellSize, paint);
							}
							fromRight = !fromRight;	
							top += cellSize + spacing;
						}
						top += blockSpacing - spacing;
					}
					
					c.drawBitmap(solveBtn, solveX, sby, null);
					c.drawBitmap(keyPad, gridX, kpy, null);
					c.drawBitmap(recycleBin, binX, bby, null);
					
					holder.unlockCanvasAndPost(c);
				}
			}
		}

		cell.recycle();
		keyPad.recycle();
		solveBtn.recycle();
		c = null;
	}
	
	private void calculateDimensions(int screenWidth)
	{
		gridSize = (int) (relGridSize * screenWidth);
		spacing = (int) (relSpacing * screenWidth);
		spacing = spacing < 1 ? 1 : spacing;
		blockSpacing = (int) (blockSpacingFactor * spacing);
		cellSize = (gridSize - 6 * spacing - 2 * blockSpacing) / 9;
		gridSize = 9 * cellSize + 6 * spacing + 2 * blockSpacing;
	}
	
	private void render()
	{
		Canvas canvas = lockCanvas();
		if (canvas == null) return;

		canvas.drawColor(bgColor);
		
		int i, j, k, w;
		int counter = 0;
		int number = 0;
		
		int cellCenterX = cellSize / 2;
		int cellCenterY = 3 * cellSize / 4;
		
		int x = gridX;
		int y = gridY;
		
		for (i = 0; i < 3; i++)
		{
			for (j = 0; j < 3; j++)
			{
				for (k = 0; k < 3; k++)
				{
					for (w = 0; w < 3; w++)
					{
						if (counter == selectedIndex)
							paint.setColor(selectColor);
						else
							paint.setColor(cellColor);
						
						canvas.drawRect(x, y, x + cellSize, y + cellSize, paint);
						
						if (redCells[counter])
						{
							paint.setColor(0x99FF0000);
							canvas.drawRect(x, y, x + cellSize, y + cellSize, paint);
						}
							
						number = grid[counter];
						if (number != 0)
						{
							if (solvedCells[counter])
								paint.setColor(solvedNumColor);
							else
								paint.setColor(keyPadNumColor);
							
							canvas.drawText(String.valueOf(number), x + cellCenterX, y + cellCenterY, paint);
						}
							
						x += cellSize + spacing;
						counter++;
					}
					
					x += blockSpacing - spacing;
				}
			
				x -= gridSize + blockSpacing;
				y += cellSize + spacing;
			}
			
			y += blockSpacing - spacing;
		}
		
		// Draw key pad				
		x = gridX;
		y = keyPadY + cellSize + blockSpacing;
		counter = 1;

		for (i = 0; i < 3; i++)
		{
			for (j = 0; j < 3; j++)
			{
				paint.setColor(keyPadColor);
				canvas.drawRect(x, y, x + cellSize, y + cellSize, paint);
				
				paint.setColor(keyPadNumColor);
				canvas.drawText(String.valueOf(counter), x + cellCenterX, y + cellCenterY, paint);
				
				x += cellSize + spacing;
				counter++;
			}
			
			x += blockSpacing - spacing;
		}
		
		// Draw clear button
		paint.setColor(keyPadColor);
		x = gridX + 3 * cellSize + 2 * spacing + blockSpacing;
		canvas.drawRect(x, keyPadY, x + 3 * cellSize + 2 * spacing, keyPadY + cellSize, paint);
		
		paint.setColor(keyPadNumColor);
		canvas.drawText("CLEAR", x + 3 * cellSize / 2 + spacing, keyPadY + 3 * cellSize / 4, paint);
		
		// Draw recycle bin
		canvas.drawBitmap(recycleBin, binX, binY, null);
		
		// Draw solve button
		if (selectedNumber == 10)
		{
			paint.setColor(keyPadColor);
			canvas.drawRect(solveX, solveY, solveX + solveWidth, solveY + solveHeight, paint);
		
			paint.setColor(keyPadNumColor);
			canvas.drawText("SOLVE", solveX + solveWidth / 2, solveY + 3 * solveHeight / 4, paint);	
			holder.unlockCanvasAndPost(canvas);
			shakeSolveButton();		
		}
		else
		{
			paint.setColor(keyPadColor);
			canvas.drawRect(solveX, solveY, solveX + solveWidth, solveY + solveHeight, paint);
		
			paint.setColor(keyPadNumColor);
			canvas.drawText("SOLVE", solveX + solveWidth / 2, solveY + 3 * solveHeight / 4, paint);
			holder.unlockCanvasAndPost(canvas);
		}	
	}
	
	private void shakeSolveButton()
	{
		Canvas canvas;
		
		boolean running = true;
		
		long accumulator = 0L;
		long frameTime = 1000000000L / 60;
		long now = System.nanoTime();
		long lastTime = now;
		
		float t = 0.0f;
		float delta = frameTime / 1000000000.0f;
		float x = 0.0f;
		
		while (running)
		{
			now = System.nanoTime();
			accumulator += now - lastTime;
			lastTime = now;
			
			if (accumulator >= frameTime)
			{	
				accumulator -= frameTime * (accumulator / frameTime);
				
				x = amplitude * (1 - t / shakeAnimTime) * (float) Math.sin(2 * Math.PI * frequency * t);
				t += delta;
				
				if (t >= shakeAnimTime)
					running = false;
				
				canvas = null;
				if (surfaceValid) canvas = holder.lockCanvas();
				if (canvas != null)
				{
					canvas.clipRect(solveX - amplitude, solveY, solveX + solveWidth + amplitude, solveY + solveHeight);
					
					canvas.drawColor(bgColor);
					paint.setColor(keyPadColor);
					canvas.drawRect(solveX + x, solveY, solveX + x + solveWidth, solveY + solveHeight, paint);
					paint.setColor(keyPadNumColor);
					canvas.drawText("SOLVE", solveX + solveWidth / 2 + x, solveY + 3 * solveHeight / 4, paint);
					
					holder.unlockCanvasAndPost(canvas);
				}
			}
		}
	}
	
	// TODO: Finish this
	private void placeNumber(int index, int number)
	{
		int i, j, k, w, u, v;
		int gridOffset;
		int squareOffset;
		int squareStart;
		
		grid[index] = number;
		solvedCells[index] = false;
		
		for (i = 0; i < 81; i++)
			redCells[i] = false;
			
		boolean[] counters = new boolean[9];
		
		for (i = 0; i < 9; i++)
		{
			for (j = 0; j < 9; j++)
				counters[j] = false;
			
			for (j = 0; j < 9; j++)
			{
				if (grid[j + i * 9] != 0)
				{
					if (counters[grid[j + i * 9] - 1])
					{
						for (k = 0; k < 9; k++)
							redCells[k + i * 9] = true;
						break;
					}
					else
						counters[grid[j + i * 9] - 1] = true;
				}
			}
		}
		
		for (i = 0; i < 9; i++)
		{
			for (j = 0; j < 9; j++)
				counters[j] = false;
			
			for (j = 0; j < 9; j++)
			{
				if (grid[i + j * 9] != 0)
				{
					if (counters[grid[i + j * 9] - 1])
					{
						for (k = 0; k < 9; k++)
							redCells[i + k * 9] = true;
						break;
					}
					else
						counters[grid[i + j * 9] - 1] = true;
				}
			}
		}
		
		gridOffset = 0;
		
		for (i = 0; i < 3; i++)
		{
			for (j = 0; j < 3; j++)
			{
				for (k = 0; k < 9; k++)
					counters[k] = false;
			
				squareStart = gridOffset;
				
				for (k = 0; k < 3; k++)
				{
					for (w = 0; w < 3; w++)
					{
						if (grid[gridOffset] != 0)
						{
							if (counters[grid[gridOffset] - 1])
							{
								squareOffset = squareStart;
								
								for (u = 0; u < 3; u++)
								{
									for (v = 0; v < 3; v++)
									{
										redCells[squareOffset] = true;
										squareOffset++;
									}
									
									squareOffset += 6;
								}
							}
							else
								counters[grid[gridOffset] - 1] = true;
						}
						
						gridOffset++;
					}
					
					gridOffset += 6;
				}
				
				gridOffset -= 24;
			}
			
			gridOffset += 18;
		}
	}

	private void clearGrid()
	{
		int i;
		for (i = 0; i < 81; i++)
		{
			grid[i] = 0;
			redCells[i] = false;
			solvedCells[i] = false;
		}
	}
	
	private boolean solve()
	{
		int i;

		int[] newGrid = new int[9 * 9];
		
		for (i = 0; i < 81; i++)
			newGrid[i] = grid[i];
		
		SudokuSolver solver = new SudokuSolver(newGrid);
		
		boolean result = solver.solve();
			
		if (result)
		{
			for (i = 0; i < 81; i++)
				if (newGrid[i] != grid[i])
					solvedCells[i] = true;

			grid = newGrid;
		}

		return result;
	}
	
	private void setTextSize(String text, float height, Paint paint)
	{
		final float testTextSize = 48.0f;
		paint.setTextSize(testTextSize);
		
		Rect bounds = new Rect();
		paint.getTextBounds(text, 0, text.length(), bounds);
		
		float desiredTextSize = testTextSize * height / bounds.height();
		paint.setTextSize(desiredTextSize);
	}

	private Canvas lockCanvas()
	{
		if (surfaceValid)
			return holder.lockCanvas();
		else
			return null;
	}
	
	//TODO: Clean up
	private void processInput (float x, float y, int e)
	{
		if (e != MotionEvent.ACTION_DOWN)
			return;

		if (x >= binX && x < binX + binWidth)
			if (y >= binY && y < binY + binHeight)
				clearGrid();
		
		x -= gridX;
		
		if (x >= 0 && x < gridSize)
		{
			if (y >= solveY && y < solveY + solveHeight)
			{
				if (x >= solveX && x < solveX + solveWidth)
				{
					if (!solve())
						selectedNumber = 10;
				}
			}
				
			else if (y >= gridY && y < gridY + gridSize)
			{
				y -= gridY;
				intersectsCell(x, y);
			}

		
			else if (y >= keyPadY && y < keyPadY + 2 * cellSize + blockSpacing)
			{
				y -= keyPadY;
				if (y >= 0 && y <= cellSize)
				{
					if (x >= 3 * cellSize + 2 * spacing + blockSpacing && x < 6 * cellSize + 4 * spacing + blockSpacing)
						selectedNumber = 0;
				}
				if (y >= cellSize + blockSpacing)
				{
					y -= cellSize + blockSpacing;
					intersectsNumber(x);
				}
			}
		}
	}
	
	//TODO: Fix this
	private void intersectsCell(float x, float y)
	{
		int h1 = 3 * cellSize + 2 * spacing + blockSpacing;
		int h2 = cellSize + spacing;
		
		int m = spacing - blockSpacing;
		
		int xIndex = ((int) x + spacing + (((int) x + blockSpacing) / h1) * m) / h2;
		int yIndex = ((int) y + spacing + (((int) y + blockSpacing) / h1) * m) / h2;
	
		selectedIndex = xIndex + yIndex * 9;
	}

	//TODO: Optimize this too
	private void intersectsNumber(float x)
	{
		if (x < 3 * cellSize + 2 * spacing) {
			if (x < cellSize)
				selectedNumber = 1;
			else if (x < 2 * cellSize + spacing)
				selectedNumber = 2;
			else
				selectedNumber = 3;
		} else if (x < 6 * cellSize + 4 * spacing + blockSpacing) {
			x -= 3 * cellSize + 2 * spacing + blockSpacing;
			if (x < cellSize)
				selectedNumber = 4;
			else if (x < 2 * cellSize + spacing)
				selectedNumber = 5;
			else
				selectedNumber = 6;
		} else {
			x -= 6 * cellSize + 4 * spacing + 2 * blockSpacing;
			if (x < cellSize)
				selectedNumber = 7;
			else if (x < 2 * cellSize + spacing)
				selectedNumber = 8;
			else
				selectedNumber = 9;
		}
	}
}
