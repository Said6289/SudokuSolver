package com.saidalattrach.sudokusolver;

public class SudokuSolver
{
	private int[] grid;

	// pm : Possibility Matrix
	private boolean[] pm = new boolean[9 * 9 * 9];

	public SudokuSolver()
	{
		this(new int[9 * 9]);
	}

	public SudokuSolver(int[] grid)
	{
		this.grid = grid;
	}

	public int[] getGrid()
	{
		return grid;
	}

	public boolean[] getPM()
	{
		return pm;
	}

	public void setGrid(int[] grid)
	{
		this.grid = grid;
	}

	public boolean solve()
	{
		if (!validateGrid())
			return false;

		initializePM();

		int numsPlaced = 1;

		while (numsPlaced > 0)
		{
			numsPlaced = 0;
			numsPlaced += simpleEliminate();
			numsPlaced += lessSimpleEliminate();
		}

		return checkAndBranch();
	}

	private boolean validateGrid()
	{
		int i, j, k, w;
		int gridOffset;

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
						return false;
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
						return false;
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
							
				for (k = 0; k < 3; k++)
				{
					for (w = 0; w < 3; w++)
					{
						if (grid[gridOffset] != 0)
						{
							if (counters[grid[gridOffset] - 1])
								return false;
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

		return true;
	}

	private boolean checkAndBranch()
	{
		int i, j;

		// Finds the un-filled cell with the least possibilities
		int possibsCounter;
		int leastPossibs = 10;
		int leastPossibsIndex = -1;

		for (i = 0; i < 81; i++)
		{
			if (grid[i] == 0)
			{
				possibsCounter = 0;

				for (j = 0; j < 9; j++)
				{
					if (pm[j + i * 9] == true)
						possibsCounter++;
				}
				if (possibsCounter < leastPossibs)
				{
					leastPossibs = possibsCounter;
					leastPossibsIndex = i;
				}
			}
		}

		// This means that all cells are filled and puzzle is solved
		if (leastPossibs == 10)
			return true;

		// This means that the puzzle is invalid
		if (leastPossibs == 0)
			return false;

		int[] possibs = new int[leastPossibs];
		int counter = 0;

		for (i = 0; i < 9; i++)
		{
			if (pm[leastPossibsIndex * 9 + i] == true)
			{
				possibs[counter] = i + 1;
				counter++;
			}
		}

		int[] oldGrid = new int[81];
		
		for (i = 0; i < 81; i++)
			oldGrid[i] = grid[i];

		for (i = 0; i < leastPossibs; i++)
		{
			grid[leastPossibsIndex] = possibs[i];

			boolean result = solve();
			
			if (result)
				return true;

			for (j = 0; j < 81; j++)
				grid[j] = oldGrid[j];
		}

		return false;
	}

	// Places a number in the grid and updates the possibility matrix accordingly
	private void placeNumber(int index, int number)
	{
		// Place the number in the grid
		grid[index] = number;

		// Calulate row and column of this number
		int row = index / 9;
		int col = index % 9;

		// Calculate starting index of this number's 3x3 square
		int squareStart = (col - (col % 3)) + (row - (row % 3)) * 9;

		int i, j;
		int gridOffset;
		int pmOffset;

		// Remove this number from the possibilities of each cell in the row...
		pmOffset = number - 1 + row * 81;

		for (i = row * 9; i < (row + 1) * 9; i++)
		{
			if (grid[i] == 0)
				pm[pmOffset] = false;

			pmOffset += 9;
		}

		// ...column....
		pmOffset = number - 1 + col * 9;

		for (i = col; i < col + 81; i += 9)
		{
			if (grid[i] == 0)
				pm[pmOffset] = false;

			pmOffset += 81;
		}

		// ...and 3x3 square
		gridOffset = squareStart;
		pmOffset = number - 1 + squareStart * 9;

		for (i = 0; i < 3; i++)
		{
			for (j = 0; j < 3; j++)
			{
				if (grid[gridOffset] == 0)
					pm[pmOffset] = false;
				
				gridOffset++;
				pmOffset += 9;
			}

			gridOffset += 6;
			pmOffset += 54;
		}

		// Remove all other possibilities from this cell
		for (i = 0; i < 9; i++)
			pm[index * 9 + i] = false;
		
		pm[index * 9 + number - 1] = true;
	}

	private void initializePM()
	{
		int i;

		for (i = 0; i < 9 * 9 * 9; i++)
			pm[i] = true;

		for (i = 0; i < 9 * 9; i++)
			if (grid[i] != 0)
				placeNumber(i, grid[i]);
	}

	private int simpleEliminate()
	{
		int counter = 0;
	
		boolean onlyOnePossib;
		int single = 0;

		int i, j;

		for (i = 0; i < 81; i++)
		{
			if (grid[i] == 0)
			{
				onlyOnePossib = false;

				for (j = 0; j < 9; j++)
				{
					if (pm[j + i * 9])
					{
						if (onlyOnePossib)
						{
							onlyOnePossib = false;
							break;
						}

						onlyOnePossib = true;
						single = j + 1;
					}
				}

				if (onlyOnePossib)
				{
					placeNumber(i, single);
					counter++;
				}
			}
		}
		
		return counter;
	}

	private int lessSimpleEliminate()
	{
		int counter = 0;

		boolean[] counters = new boolean[9];
		int[] indices = new int[9];

		int index;
		int gridOffset;
		int i, j, k, w, v;

		// Check rows
		for (k = 0; k < 9; k++)
		{
			for (i = 0; i < 9; i++)
				counters[i] = false;

			for (i = 0; i < 9; i++)
				indices[i] = -1;

			for (i = 0; i < 9; i++)
			{
				if (grid[i + k * 9] == 0)
				{
					for (j = 0; j < 9; j++)
					{
						if (pm[j + i * 9 + k * 81] == true)
						{
							if (counters[j] == true)
								indices[j] = -1;
							else
							{
								counters[j] = true;
								indices[j] = i;
							}
						}
					}
				}
			}

			for (i = 0; i < 9; i++)
			{
				index = indices[i];
				if (index != -1)
				{
					for (j = 0; j < 9; j++)
						pm[j + index * 9 + k * 81] = false;

					pm[i + index * 9 + k * 81] = true;

					placeNumber(index + k * 9, i + 1);
					counter++;
				}
			}
		}

		// Check columns
		for (k = 0; k < 9; k++)
		{
			for (i = 0; i < 9; i++)
				counters[i] = false;

			for (i = 0; i < 9; i++)
				indices[i] = -1;

			for (i = 0; i < 9; i++)
			{
				if (grid[k + i * 9] == 0)
				{
					for (j = 0; j < 9; j++)
					{
						if (pm[j + k * 9 + i * 81] == true)
						{
							if (counters[j] == true)
								indices[j] = -1;
							else
							{
								counters[j] = true;
								indices[j] = i;
							}
						}
					}
				}
			}

			for (i = 0; i < 9; i++)
			{
				index = indices[i];
				if (index != -1)
				{
					for (j = 0; j < 9; j++)
					{
						pm[j + k * 9 + index * 81] = false;
					}

					pm[i + k * 9 + index * 81] = true;

					placeNumber(k + index * 9, i + 1);
					counter++;
				}
			}
		}

		// Check 3x3 squares
		gridOffset = 0;

		for (w = 0; w < 3; w++)
		{
			for (k = 0; k < 3; k++)
			{
				for (i = 0; i < 9; i++)
					counters[i] = false;

				for (i = 0; i < 9; i++)
					indices[i] = -1;

				for (j = 0; j < 3; j++)
				{
					for (i = 0; i < 3; i++)
					{
						if (grid[gridOffset] == 0)
						{
							for (v = 0; v < 9; v++)
							{
								if (pm[v + gridOffset * 9] == true)
								{
									if (counters[v] == true)
										indices[v] = -1;
									else
									{
										counters[v] = true;
										indices[v] = gridOffset;
									}
								}
							}
						}

						gridOffset++;
					}

					gridOffset += 6;
				}

				for (i = 0; i < 9; i++)
				{
					index = indices[i];
					if (index != -1)
					{
						for (j = 0; j < 9; j++)
							pm[j + index * 9] = false;

						pm[i + index * 9] = true;
						placeNumber(index, i + 1);
						counter++;
					}
				}

				gridOffset -= 24;
			}

			gridOffset += 18;
		}

		return counter;
	}

}