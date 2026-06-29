package ml

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"strings"
	"time"
)

const analyzePath = "/internal/ml/analyze"

var ErrServiceUnavailable = errors.New("ml service unavailable")

type Client struct {
	baseURL    string
	httpClient *http.Client
}

func NewClient(baseURL string) *Client {
	return &Client{
		baseURL: strings.TrimRight(baseURL, "/"),
		httpClient: &http.Client{
			Timeout: 15 * time.Second,
		},
	}
}

func (c *Client) AnalyzeImage(ctx context.Context, image io.Reader, filename string) (AnalyzeResponse, error) {
	var requestBody bytes.Buffer

	writer := multipart.NewWriter(&requestBody)

	fileWriter, err := writer.CreateFormFile("image", filename)
	if err != nil {
		return AnalyzeResponse{}, err
	}

	if _, err := io.Copy(fileWriter, image); err != nil {
		return AnalyzeResponse{}, err
	}

	if err := writer.Close(); err != nil {
		return AnalyzeResponse{}, err
	}

	request, err := http.NewRequestWithContext(
		ctx,
		http.MethodPost,
		c.baseURL+analyzePath,
		&requestBody,
	)
	if err != nil {
		return AnalyzeResponse{}, err
	}

	request.Header.Set("Content-Type", writer.FormDataContentType())

	response, err := c.httpClient.Do(request)
	if err != nil {
		return AnalyzeResponse{}, fmt.Errorf("%w: %v", ErrServiceUnavailable, err)
	}
	defer response.Body.Close()

	if response.StatusCode != http.StatusOK {
		return AnalyzeResponse{}, fmt.Errorf("%w: status code %d", ErrServiceUnavailable, response.StatusCode)
	}

	var analyzeResponse AnalyzeResponse

	if err := json.NewDecoder(response.Body).Decode(&analyzeResponse); err != nil {
		return AnalyzeResponse{}, fmt.Errorf("%w: invalid response body", ErrServiceUnavailable)
	}

	if analyzeResponse.Status != "success" {
		return AnalyzeResponse{}, fmt.Errorf("%w: invalid response status", ErrServiceUnavailable)
	}

	return analyzeResponse, nil
}
