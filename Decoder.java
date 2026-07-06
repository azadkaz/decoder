import java.io.*;
import java.util.*;
import java.net.URL;

public class Decoder {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("error");
            return;
        }

        String urlString = args[0];
        String docId = extractDocId(urlString);
        String exportUrl = buildExportUrl(urlString, docId);

        StringBuilder data = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(exportUrl).openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                data.append(line).append("\n");
            }
        } catch (IOException e) {
            System.out.println("error");
            return;
        }

        Map<Integer, Map<Integer, Character>> grid = new HashMap<>();
        String[] lines = data.toString().split("\n");

        
        parseHtmlTable(data.toString(), grid);
        printGrid(grid);
    }
    
    private static String extractDocId(String urlString) {
        // For Google Docs: /document/d/e/{ID}/
        // For Google Sheets: /spreadsheets/d/{ID}/
        
        if (urlString.contains("/document/d/e/")) {
            int start = urlString.indexOf("/document/d/e/") + 14;
            int end = urlString.indexOf("/", start);
            if (end == -1) {
                end = urlString.length();
            }
            return urlString.substring(start, end);
        }
        return null;
    }
    
    private static String buildExportUrl(String urlString, String docId) {
        if (urlString.contains("/document/")) {
            // For Google Docs, just return the public URL as-is
            // The content will be HTML that we can parse
            return urlString;
        }
        return null;
    }
    
    private static void printGrid(Map<Integer, Map<Integer, Character>> grid) {
        if (grid.isEmpty()) {
            System.out.println("error");
            return;
        }

        int minY = Collections.min(grid.keySet());
        int maxY = Collections.max(grid.keySet());
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;

        for (Map<Integer, Character> row : grid.values()) {
            if (!row.isEmpty()) {
                minX = Math.min(minX, Collections.min(row.keySet()));
                maxX = Math.max(maxX, Collections.max(row.keySet()));
            }
        }

        for (int y = minY; y <= maxY; y++) {
            StringBuilder line = new StringBuilder();
            Map<Integer, Character> row = grid.getOrDefault(y, new HashMap<>());
            for (int x = minX; x <= maxX; x++) {
                line.append(row.getOrDefault(x, ' '));
            }
            System.out.println(line.toString());
        }
    }
    
    private static void parseHtmlTable(String html, Map<Integer, Map<Integer, Character>> grid) {
        // Extract table rows from HTML
        int tableStart = html.indexOf("<table");
        if (tableStart == -1) {
            return;
        }
        
        int tableEnd = html.indexOf("</table>", tableStart);
        if (tableEnd == -1) {
            tableEnd = html.length();
        }
        
        String tableHtml = html.substring(tableStart, tableEnd);
        
        // Find all rows
        int rowStart = 0;
        int rowNum = 0;
        while ((rowStart = tableHtml.indexOf("<tr", rowStart)) != -1) {
            int rowEnd = tableHtml.indexOf("</tr>", rowStart);
            if (rowEnd == -1) break;
            
            String rowHtml = tableHtml.substring(rowStart, rowEnd);
            rowNum++;
            
            // Skip header row
            if (rowNum == 1) {
                rowStart = rowEnd + 5;
                continue;
            }
            
            // Extract cells from this row
            int cellStart = 0;
            int cellNum = 0;
            String[] cells = new String[3];
            
            while ((cellStart = rowHtml.indexOf("<td", cellStart)) != -1) {
                int cellEnd = rowHtml.indexOf("</td>", cellStart);
                if (cellEnd == -1) break;
                
                // Find the content between > and </td>
                int contentStart = rowHtml.indexOf(">", cellStart) + 1;
                String cellContent = rowHtml.substring(contentStart, cellEnd).replaceAll("<[^>]+>", "").trim();
                
                if (cellNum < 3) {
                    cells[cellNum] = cellContent;
                }
                cellNum++;
                cellStart = cellEnd + 5;
            }
            
            // Parse the three cells
            if (cells[0] != null && cells[1] != null && cells[2] != null) {
                try {
                    int x = Integer.parseInt(cells[0].trim());
                    char c = cells[1].trim().charAt(0);
                    int y = Integer.parseInt(cells[2].trim());
                    grid.computeIfAbsent(y, k -> new HashMap<>()).put(x, c);
                } catch (Exception e) {
                }
            }
            
            rowStart = rowEnd + 5;
        }
    }
}