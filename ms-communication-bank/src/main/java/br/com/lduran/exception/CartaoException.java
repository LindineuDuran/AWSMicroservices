package br.com.lduran.exception;

public class CartaoException extends RuntimeException
{
	private static final long serialVersionUID = 8011193673466440726L;

	public CartaoException(String message)
	{
		super(message);
	}
}