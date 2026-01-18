/**
 * Built-in plugins that come bundled with Forevernote.
 * 
 * <p>This package contains official plugins that provide additional
 * functionality out of the box:</p>
 * 
 * <ul>
 *   <li>{@link com.example.forevernote.plugin.builtin.WordCountPlugin} - 
 *       Word and character statistics</li>
 *   <li>{@link com.example.forevernote.plugin.builtin.DailyNotesPlugin} - 
 *       Daily note creation and management</li>
 *   <li>{@link com.example.forevernote.plugin.builtin.ReadingTimePlugin} - 
 *       Reading time estimation</li>
 * </ul>
 * 
 * <h2>Creating Custom Plugins</h2>
 * <p>To create a custom plugin, implement the 
 * {@link com.example.forevernote.plugin.Plugin} interface and register
 * it with the {@link com.example.forevernote.plugin.PluginManager}.</p>
 * 
 * <pre>{@code
 * public class MyPlugin implements Plugin {
 *     @Override
 *     public String getId() { return "my-plugin"; }
 *     
 *     @Override
 *     public String getName() { return "My Plugin"; }
 *     
 *     @Override
 *     public String getVersion() { return "1.0.0"; }
 *     
 *     @Override
 *     public void initialize(PluginContext context) {
 *         context.registerCommand("My Command", "Description", () -> {
 *             // Your action here
 *         });
 *     }
 *     
 *     @Override
 *     public void shutdown() {
 *         // Cleanup
 *     }
 * }
 * }</pre>
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.2.0
 */
package com.example.forevernote.plugin.builtin;
